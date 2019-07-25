package com.stable.vo;

import java.util.Date;
import java.util.List;

import lombok.Data;

@Data
public class UserVo {// implements Serializable{
	/**
	 * 
	 */
	// private static final long serialVersionUID = 3102074407008461165L;

	private int id;
	private String username;
	private int age;
	private Date ctm;
	private List<Object> nullarr;
	private Object nullstr;
}
