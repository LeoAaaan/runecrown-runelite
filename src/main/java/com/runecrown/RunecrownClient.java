package com.runecrown;

import com.google.gson.Gson;
import java.io.IOException;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

// Talks to the Runecrown server with the member's own overlay code. Read the feed, answer an event, and the opt-in reports (drop races, clan feed).
@Slf4j
@Singleton
class RunecrownClient
{
	static final java.util.regex.Pattern CODE = java.util.regex.Pattern.compile("^ro_[a-z0-9-]{3,32}_[a-z0-9]{20,40}$");

	private final OkHttpClient http;
	private final Gson gson;

	@Inject
	RunecrownClient(OkHttpClient http, Gson gson)
	{
		this.http = http;
		this.gson = gson;
	}

	private HttpUrl.Builder base(String server, String path, String code)
	{
		HttpUrl u = HttpUrl.parse(server);
		if (u == null)
		{
			u = HttpUrl.parse("https://guild-hub-bot.holmen-moe.workers.dev");
		}
		return u.newBuilder().addPathSegments(path).addQueryParameter("code", code);
	}

	void feed(String server, String code, Consumer<Feed> done)
	{
		Request req = new Request.Builder().url(base(server, "overlay/feed", code).build()).header("Cache-Control", "no-store").build();
		http.newCall(req).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.debug("Runecrown feed failed", e);
				done.accept(error("Could not reach Runecrown. Trying again soon."));
			}

			@Override
			public void onResponse(Call call, Response res)
			{
				done.accept(read(res));
			}
		});
	}

	void answer(String server, String code, String eventId, String status, Consumer<Feed> done)
	{
		HttpUrl url = base(server, "overlay/signup", code).addQueryParameter("event", eventId).addQueryParameter("status", status).build();
		Request req = new Request.Builder().url(url).put(RequestBody.create(null, new byte[0])).build();
		http.newCall(req).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				done.accept(error("Could not reach Runecrown. Try again."));
			}

			@Override
			public void onResponse(Call call, Response res)
			{
				done.accept(read(res));
			}
		});
	}

	// A drop that matches a running drop race. The server decides who was first.
	void drop(String server, String code, String raceId, String source, String item, Integer kc, Integer raceKills, String rsn, Consumer<Feed> done)
	{
		java.util.Map<String, Object> body = new java.util.HashMap<>();
		body.put("race", raceId);
		body.put("source", source);
		body.put("item", item);
		body.put("kc", kc);
		body.put("raceKills", raceKills);
		body.put("rsn", rsn);
		Request req = new Request.Builder().url(base(server, "overlay/drop", code).build())
			.post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), gson.toJson(body))).build();
		http.newCall(req).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.debug("Runecrown drop report failed", e);
				done.accept(error("Could not reach Runecrown to report your drop."));
			}

			@Override
			public void onResponse(Call call, Response res)
			{
				done.accept(read(res));
			}
		});
	}

	// Clan feed: a valuable drop, pet, new collection log item, 99 or personal best. Body fields as in /overlay/osrs.
	void osrs(String server, String code, java.util.Map<String, Object> body, Consumer<Feed> done)
	{
		Request req = new Request.Builder().url(base(server, "overlay/osrs", code).build())
			.post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), gson.toJson(body))).build();
		http.newCall(req).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.debug("Runecrown clan feed report failed", e);
				done.accept(error("Could not reach Runecrown to share that."));
			}

			@Override
			public void onResponse(Call call, Response res)
			{
				done.accept(read(res));
			}
		});
	}

	// Any JSON POST to an overlay path (clan list, clan chat), answered as a Feed.
	void post(String server, String path, String code, Object body, String failMessage, Consumer<Feed> done)
	{
		Request req = new Request.Builder().url(base(server, path, code).build())
			.post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), gson.toJson(body))).build();
		http.newCall(req).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.debug("Runecrown {} failed", path, e);
				done.accept(error(failMessage));
			}

			@Override
			public void onResponse(Call call, Response res)
			{
				done.accept(read(res));
			}
		});
	}

	// Discord lines for the in-game chat box, newer than "after" (none the first time: it starts from now).
	void chat(String server, String code, String after, Consumer<Feed> done)
	{
		HttpUrl.Builder u = base(server, "overlay/chat", code);
		if (after != null)
		{
			u.addQueryParameter("after", after);
		}
		Request req = new Request.Builder().url(u.build()).header("Cache-Control", "no-store").build();
		http.newCall(req).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				done.accept(error("Could not reach Runecrown."));
			}

			@Override
			public void onResponse(Call call, Response res)
			{
				done.accept(read(res));
			}
		});
	}

	// Find a clan: the public directory of Old School RuneScape clans taking applications. No code needed.
	void directory(String server, Consumer<Feed> done)
	{
		HttpUrl u = HttpUrl.parse(server);
		if (u == null)
		{
			u = HttpUrl.parse("https://guild-hub-bot.holmen-moe.workers.dev");
		}
		Request req = new Request.Builder().url(u.newBuilder().addPathSegment("directory").addQueryParameter("game", "osrs").build()).build();
		http.newCall(req).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				done.accept(error("Could not reach Runecrown. Try again."));
			}

			@Override
			public void onResponse(Call call, Response res)
			{
				done.accept(read(res));
			}
		});
	}

	private Feed read(Response res)
	{
		try (ResponseBody body = res.body())
		{
			Feed f = body == null ? null : gson.fromJson(body.charStream(), Feed.class);
			if (f == null)
			{
				return error("Runecrown sent nothing back.");
			}
			if (!res.isSuccessful() && f.message == null)
			{
				f.message = res.code() == 401 ? "This code no longer works. Make a new one in the hub." : "Runecrown is busy. Trying again soon.";
			}
			if (!res.isSuccessful() && f.error == null)
			{
				f.error = "http_" + res.code();
			}
			return f;
		}
		catch (Exception e)
		{
			log.debug("Runecrown answer unreadable", e);
			return error("Runecrown sent something odd back.");
		}
	}

	static Feed error(String message)
	{
		Feed f = new Feed();
		f.error = "error";
		f.message = message;
		return f;
	}
}
