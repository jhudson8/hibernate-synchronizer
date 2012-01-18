package com.hudson.hibernatesynchronizer.util;

public interface IErrorHandler {

	public void onError (String message, Throwable t);
}
