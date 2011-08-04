package com.syncapse.jenkinsci.plugins.awscloudformationwrapper;

public class TimeoutException extends RuntimeException {

	private static final long serialVersionUID = 7487597679274146083L;

	public TimeoutException() {
		super();
	}

	public TimeoutException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public TimeoutException(String arg0) {
		super(arg0);
	}

	public TimeoutException(Throwable arg0) {
		super(arg0);
	}

}
