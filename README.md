# Redis Scheduler

This is a redis-backed Scheduler-as-a-Service, which allows one to submit scheduled jobs and get callbacks at the scheduled times. 

Jobs can be POST-ed at the `/schedule` rest endpoint.

The wire format for a one-off scheduled job: 

```
{  
    "callback" : "http://localhost:8080/callback",
    "payload" : "foo"
}
```

Jobs are stored in redis, and scanned every second. Once a job is triggered, you'll get a callback at the provided url with the given payload. Simple. 

Scheduling recurring jobs is also supported, in the java fixed-delay ScheduledExecutorService style (not arbitrary cron expressions). 
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

Or a recurring job:

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
