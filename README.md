# Redis Scheduler

This is a redis-backed Scheduler-as-a-Service, which allows one to submit scheduled jobs and get callbacks at the scheduled times. 

Jobs can be POST-ed at the ```/schedule``` rest endpoint.

The wire format for a one-off scheduled job: 

```
{  
   callback= http://localhost:8080/callback,
   payload={myPayload=probablyJsonButWhateverYouWantIsFine},
   scheduledTime=1483832309634
}
```
.. where scheduledTime is in milliseconds since epoch. 

Jobs are stored in redis, and scanned every second. Once a job is triggered, you'll get a callback with the given payload. Simple. 

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

That's all folks. 
