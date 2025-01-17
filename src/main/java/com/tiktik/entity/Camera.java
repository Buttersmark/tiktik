package com.tiktik.entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("camera")
@Entity
@Table(name="camera")	//jpa自动创建表
public class Camera implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5575352151805386129L;
	
	@Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
	
	@Column
	private String url;
	@Column
	private String remark;
	@Column
	private int flv;
	@Column
	private int ffmpeg;
	@Column
	private int autoClose;
	@Column
	private int type = 0;
	@Column
	private String mediaKey;
}
