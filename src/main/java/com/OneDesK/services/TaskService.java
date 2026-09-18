package com.OneDesK.services;

public interface TaskService {

	/** Programa la tarea para que Spring la ejecute en su pool de hilos cada vez que se cumpla la expresion cron. */
	public void scheduleTask(Runnable task, String cronExpression);
}
