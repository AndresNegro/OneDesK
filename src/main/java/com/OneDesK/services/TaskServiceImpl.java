package com.OneDesK.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

@Service
public class TaskServiceImpl implements TaskService {

	// lo crea Spring Boot gracias a @EnableScheduling: es un pool de hilos que ejecuta las tareas programadas
	@Autowired
	private TaskScheduler scheduler;

	@Override
	public void scheduleTask(Runnable task, String cronExpression) {
		CronTrigger trigger = new CronTrigger(cronExpression);
		scheduler.schedule(task, trigger);
	}
}
