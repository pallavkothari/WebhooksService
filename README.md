# Webhooks Scheduler

This is a redis-backed Webhooks-as-a-Service, which allows one to schedule one-off or recurring webhooks. 

Webhooks can be POST-ed at the `/schedule` rest endpoint.

The wire format for a one-off webhook: 

```
{  
    "callback" : "http://localhost:8080/callback",
    "payload" : "foo"
}
```

Webhooks are stored in redis, and scanned every second. Once a webhook is triggered, you'll get a callback at the provided url with the given payload. Simple. 

Scheduling recurring webhooks is also supported, in the java fixed-delay ScheduledExecutorService style (not arbitrary cron expressions). 
Provide the additional recurrence information using the same rest endpoint:

```
{
	"callback" : "http://localhost:8080/callback",
	"payload" : "foo",
	"isRecurring" : true,
	"delay" : 1, 
	"timeUnit" : "SECONDS",
	"numRecurrences" : 2
}
```


---
### Setup
Requires redis, so make sure it's either running locally on port 6379, or set the ```REDIS_URL``` environment variable pointing to a remote redis. 

To run locally, just do:
```
mvn install 
sh target/bin/scheduler
```

Try it out: 
```
curl -X POST -H "Content-Type: application/json" -d '{
	"callback" : "http://localhost:8080/callback",
	"payload" : "foo"
}' "http://localhost:8080/schedule"
```

Or a recurring webhook:

```
curl -X POST -H "Content-Type: application/json"  -d '{
	"callback" : "http://localhost:8080/callback",
	"payload" : "foo",
	"isRecurring" : true,
	"delay" : 1, 
	"timeUnit" : "SECONDS",
	"numRecurrences" : 2
}' "http://localhost:8080/schedule"
```

The logs will contain output from the test callback endpoint (per execution): `payload = foo`
