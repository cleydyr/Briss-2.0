package br.ufpe.cin.if1012;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import at.laborg.briss.utils.ClusterRenderWorker;
import at.laborg.briss.model.ClusterImageData;

public aspect Worker {
	private Executor executor = Executors.newCachedThreadPool();
	public pointcut asyncOperation()
		: execution(public void ClusterRenderWorker.run());
	
	public pointcut timedOperation()
		: execution(* ClusterImageData.calculateSdOfImages(..));

	void around() : asyncOperation() {
		Runnable worker = new Runnable() {
			public void run() {
				System.out.println("aspecto");
				proceed();
			}
		};
		executor.execute(worker);
	}

	Object around() : timedOperation() {
		long startTime = System.nanoTime();
		Object r = proceed();
		long endTime = System.nanoTime();

		long duration = (endTime - startTime);
		System.out.println("Duration: " + duration/1000000 + "ms");
		return r;
	}
}