package com.hudson.hibernatesynchronizer.exceptions;

import org.w3c.dom.Node;

public class CompositeKeyException extends HibernateSynchronizerException {

	private static final long serialVersionUID = 1L;

	public CompositeKeyException(Node node, String message) {
		super(message);
		setNode(node);
	}
}