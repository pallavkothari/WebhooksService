[![Build Status](https://travis-ci.org/pallavkothari/WebhooksService.svg?branch=master)](https://travis-ci.org/pallavkothari/WebhooksService) [![Coverage Status](https://coveralls.io/repos/github/pallavkothari/WebhooksService/badge.svg?branch=master)](https://coveralls.io/github/pallavkothari/WebhooksService?branch=master)

# Webhooks Scheduler

This is a redis-backed Webhooks-as-a-Service, which allows one to schedule one-off or recurring webhooks. 

Webhooks can be POST-ed at the `/schedule` rest endpoint.

The wire format for a one-off webhook: 

```
{  
    "url" : "http://localhost:8080/test-webhook",
    "payload" : "foo",
    "scheduledTime": 1484015175277      // in millis since epoch
}
```

Webhooks are stored in redis, and scanned every second. Once a webhook is triggered, you'll get a POST at the provided url with the given payload. Simple. 

Scheduling recurring webhooks is also supported, in the java fixed-delay ScheduledExecutorService style (not arbitrary cron expressions). 
Provide the additional recurrence information using the same rest endpoint:

```
{
	"url" : "http://localhost:8080/test-webhook",
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

Try it out. For a one-off, immediate webhook:  
```
curl -X POST -H "Content-Type: application/json" -d '{
	"url" : "http://localhost:8080/test-webhook",
	"payload" : "foo"
}' "http://localhost:8080/schedule"
```

Or a recurring webhook:

```
curl -X POST -H "Content-Type: application/json"  -d '{
	"url" : "http://localhost:8080/test-webhook",
	"payload" : "foo",
	"isRecurring" : true,
	"delay" : 1, 
	"timeUnit" : "SECONDS",
	"numRecurrences" : 2
}' "http://localhost:8080/schedule"
```

The logs will contain output from the test url endpoint (per execution): `payload = foo`
