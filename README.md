# Redis Scheduler

This is a redis-backed Scheduler-as-a-Service, which allows one to submit scheduled jobs and get callbacks at the scheduled times. 

Jobs can be POST-ed at the `/schedule` rest endpoint.

The wire format for a one-off scheduled job: 

```
{  
   callback= http://localhost:8080/callback,
   payload={myPayload=probablyJsonButWhateverYouWantIsFine},
   scheduledTime=1483832309634
}
```
.. where scheduledTime is in milliseconds since epoch. 

Jobs are stored in redis, and scanned every second. Once a job is triggered, you'll get a callback at the provided url with the given payload. Simple. 

Scheduling recurring jobs is also supported, in the java fixed-delay ScheduledExecutorService style (not arbitrary cron expressions). 
Provide the additional recurrence information using the same rest endpoint:

```
{
    callback=http://localhost:8080/callback, 
    payload=myPayload, 
    scheduledTime=1483832783215, 
    isRecurring=true, 
    delay=10, 
    timeUnit=MINUTES
}
```

Optionally, you can cap the number of recurrences with the `numRecurrences` field

---
### Setup
Requires redis, so make sure it's either running locally on port 6379, or set the ```REDIS_URL``` environment variable pointing to a remote redis. 

To run locally, just do:
```
mvn install 
sh target/bin/scheduler
```


If you ran mvn install above, it included a quick demo. Sample output: 
```
*** demo : Scheduling trigger to fire in 5 seconds at Sun Jan 08 18:40:11 PST 2017
*** demo : callback acked at Sun Jan 08 18:40:12 PST 2017
```

You can replicate this manually by running `mvn -Dtest=ProcessorTest#demo test`
