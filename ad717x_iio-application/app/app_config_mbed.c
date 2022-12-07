/***************************************************************************//**
 * @file    app_config_mbed.c
 * @brief   Source file for the mbed configuration for AD717x IIO Application
********************************************************************************
* Copyright (c) 2021-22 Analog Devices, Inc.
* All rights reserved.
*
* This software is proprietary to Analog Devices, Inc. and its licensors.
* By using this software you agree to the terms of the associated
* Analog Devices Software License Agreement.
*******************************************************************************/

/******************************************************************************/
/***************************** Include Files **********************************/
/******************************************************************************/

#include "app_config.h"
#include "app_config_mbed.h"

/******************************************************************************/
/********************* Macros and Constants Definition ************************/
/******************************************************************************/

/******************************************************************************/
/******************** Variables and User Defined Data Types *******************/
/******************************************************************************/

/* UART MBED Platform Specific Init Parameters */
struct mbed_uart_init_param mbed_uart_extra_init_params = {
#if defined(USE_PHY_COM_PORT)
	.virtual_com_enable = false,
	.uart_tx_pin = UART_TX,
	.uart_rx_pin = UART_RX
#else
	.virtual_com_enable = true,
	.vendor_id = VIRTUAL_COM_PORT_VID,
	.product_id = VIRTUAL_COM_PORT_PID,
	.serial_number = VIRTUAL_COM_SERIAL_NUM
#endif
};

/* SPI MBED Platform Specific Init Parameters */
struct mbed_spi_init_param mbed_spi_extra_init_params = {
	.spi_clk_pin = SPI_SCK,
	.spi_miso_pin = SPI_HOST_SDI,
	.spi_mosi_pin = SPI_HOST_SDO,
};

/* External interrupt Mbed platform specific parameters */
struct mbed_gpio_irq_init_param mbed_trigger_gpio_irq_init_params = {
	.gpio_irq_pin = RDY_PIN,
};

/* I2C Mbed platform specific parameters */
struct mbed_i2c_init_param mbed_i2c_extra_init_params = {
	.i2c_sda_pin = I2C_SDA,
	.i2c_scl_pin = I2C_SCL
};

/******************************************************************************/
/************************** Functions Declaration *****************************/
/******************************************************************************/

/******************************************************************************/
/************************** Functions Definition ******************************/
/******************************************************************************/
