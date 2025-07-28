package com.ma_wuyan;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoLandingWater implements ModInitializer {
	public static final String MOD_ID = "auto-landing-water";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// 安全地检查开发环境标志
		boolean isDevelopment = Boolean.parseBoolean(
				System.getProperty("fabric.development", "false")
		);

		if (isDevelopment) {
			LOGGER.info("AutoLandingWater mod initialized with DEBUG logging");
		} else {
			LOGGER.info("AutoLandingWater mod initialized");
		}
	}
}