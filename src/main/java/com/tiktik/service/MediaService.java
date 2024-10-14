package com.tiktik.service;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.tiktik.common.ClientType;
import com.tiktik.dto.CameraDto;
import com.tiktik.thread.MediaTransfer;
import com.tiktik.thread.MediaTransferFlvByJavacv;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.crypto.digest.MD5;
import io.netty.channel.ChannelHandlerContext;

@Service
public class  MediaService {
	
	/**
	 * 缓存流转换线程
	 */
	public static ConcurrentHashMap<String, MediaTransfer> cameras = new ConcurrentHashMap<>();
	
	
	/**
	 * http-flv播放
	 * @param cameraDto
	 * @param ctx
	 */
	public void playForHttp(CameraDto cameraDto, ChannelHandlerContext ctx) {
		
		if (cameras.containsKey(cameraDto.getMediaKey())) {
			MediaTransfer mediaConvert = cameras.get(cameraDto.getMediaKey());
			if(mediaConvert instanceof MediaTransferFlvByJavacv) {
				MediaTransferFlvByJavacv mediaTransferFlvByJavacv = (MediaTransferFlvByJavacv) mediaConvert;
				//如果当前已经用ffmpeg，则重新拉流
				if(cameraDto.isEnabledFFmpeg()) {
					mediaTransferFlvByJavacv.setRunning(false);
					cameras.remove(cameraDto.getMediaKey());
					this.playForHttp(cameraDto, ctx);
				} else {
					mediaTransferFlvByJavacv.addClient(ctx, ClientType.HTTP);
				}
			}

		} else {
				MediaTransferFlvByJavacv mediaConvert = new MediaTransferFlvByJavacv(cameraDto);
				cameras.put(cameraDto.getMediaKey(), mediaConvert);
				ThreadUtil.execute(mediaConvert);
				mediaConvert.addClient(ctx, ClientType.HTTP);
			
		}
	}

	/**
	 * ws-flv播放
	 * @param cameraDto
	 * @param ctx
	 */
	public void playForWs(CameraDto cameraDto, ChannelHandlerContext ctx) {
		
		if (cameras.containsKey(cameraDto.getMediaKey())) {
			MediaTransfer mediaConvert = cameras.get(cameraDto.getMediaKey());
			if(mediaConvert instanceof MediaTransferFlvByJavacv) {
				MediaTransferFlvByJavacv mediaTransferFlvByJavacv = (MediaTransferFlvByJavacv) mediaConvert;
				//如果当前已经用ffmpeg，则重新拉流
				if(cameraDto.isEnabledFFmpeg()) {
					mediaTransferFlvByJavacv.setRunning(false);
					cameras.remove(cameraDto.getMediaKey());
					this.playForWs(cameraDto, ctx);
				} else {
					mediaTransferFlvByJavacv.addClient(ctx, ClientType.WEBSOCKET);
				}
			}
		} else {

				MediaTransferFlvByJavacv mediaConvert = new MediaTransferFlvByJavacv(cameraDto);
				cameras.put(cameraDto.getMediaKey(), mediaConvert);
				ThreadUtil.execute(mediaConvert);
				mediaConvert.addClient(ctx, ClientType.WEBSOCKET);	

		}
	}
	
	/**
	 * api播放
	 * @param cameraDto
	 * @return 
	 */
	public boolean playForApi(CameraDto cameraDto) {
		// 区分不同媒体
		String mediaKey = MD5.create().digestHex(cameraDto.getUrl());
		cameraDto.setMediaKey(mediaKey);
		cameraDto.setEnabledFlv(true);
		
		MediaTransfer mediaTransfer = cameras.get(cameraDto.getMediaKey());
		if (null == mediaTransfer) {

				MediaTransferFlvByJavacv mediaConvert = new MediaTransferFlvByJavacv(cameraDto);
				cameras.put(cameraDto.getMediaKey(), mediaConvert);
				ThreadUtil.execute(mediaConvert);

		}
		
		mediaTransfer = cameras.get(cameraDto.getMediaKey());
		//同步等待
		if(mediaTransfer instanceof MediaTransferFlvByJavacv) {
			MediaTransferFlvByJavacv mediaft = (MediaTransferFlvByJavacv) mediaTransfer;
			// 30秒还没true认为启动不了
			for (int i = 0; i < 60; i++) {
				if (mediaft.isRunning() && mediaft.isGrabberStatus() && mediaft.isRecorderStatus()) {
					return true;
				}
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
				}
			}
		}
		return false;
	}
	
	/**
	 * 关闭流
	 * @param cameraDto
	 */
	public void closeForApi(CameraDto cameraDto) {
		cameraDto.setEnabledFlv(false);
		
		if (cameras.containsKey(cameraDto.getMediaKey())) {
			MediaTransfer mediaConvert = cameras.get(cameraDto.getMediaKey());
			if(mediaConvert instanceof MediaTransferFlvByJavacv) {
				MediaTransferFlvByJavacv mediaTransferFlvByJavacv = (MediaTransferFlvByJavacv) mediaConvert;
				mediaTransferFlvByJavacv.setRunning(false);
				cameras.remove(cameraDto.getMediaKey());
			}
		}
	}
	
}
