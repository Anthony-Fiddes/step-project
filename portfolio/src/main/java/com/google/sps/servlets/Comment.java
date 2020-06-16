package com.google.sps.servlets;

public class Comment {
	public String content;
	public String imageURL;

	Comment(String content) {
		this.content = content;
	}

	Comment(String content, String imageURL) {
		this(content);
		this.imageURL = imageURL;
	}

	@Override
	public String toString() {
		return this.content;
	}
}
