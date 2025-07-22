package com.toddmo.bydmate.collector;

import android.content.Context;
import android.hardware.bydauto.BYDAutoEventValue;
import java.util.Arrays;
import android.hardware.bydauto.ac.AbsBYDAutoAcListener;

import com.toddmo.bydmate.client.utils.DataHolder;
import com.toddmo.bydmate.client.utils.EnvironmentUtils;
import com.toddmo.bydmate.collector.LocationTracker;
import android.hardware.bydauto.ac.BYDAutoAcDevice;
import android.hardware.bydauto.bodywork.AbsBYDAutoBodyworkListener;
import android.hardware.bydauto.bodywork.BYDAutoBodyworkDevice;
import android.hardware.bydauto.charging.AbsBYDAutoChargingListener;
import android.hardware.bydauto.charging.BYDAutoChargingDevice;
import android.hardware.bydauto.charging.ChargingTimerInfo;
import android.hardware.bydauto.doorlock.AbsBYDAutoDoorLockListener;
import android.hardware.bydauto.doorlock.BYDAutoDoorLockDevice;
import android.hardware.bydauto.energy.AbsBYDAutoEnergyListener;
import android.hardware.bydauto.energy.BYDAutoEnergyDevice;
import android.hardware.bydauto.engine.AbsBYDAutoEngineListener;
import android.hardware.bydauto.engine.BYDAutoEngineDevice;
import android.hardware.bydauto.gearbox.AbsBYDAutoGearboxListener;
import android.hardware.bydauto.gearbox.BYDAutoGearboxDevice;
import android.hardware.bydauto.instrument.AbsBYDAutoInstrumentListener;
import android.hardware.bydauto.instrument.BYDAutoInstrumentDevice;
import android.hardware.bydauto.panorama.AbsBYDAutoPanoramaListener;
import android.hardware.bydauto.panorama.BYDAutoPanoramaDevice;
import android.hardware.bydauto.radar.AbsBYDAutoRadarListener;
import android.hardware.bydauto.radar.BYDAutoRadarDevice;
import android.hardware.bydauto.sensor.AbsBYDAutoSensorListener;
import android.hardware.bydauto.sensor.BYDAutoSensorDevice;
import android.hardware.bydauto.setting.AbsBYDAutoSettingListener;
import android.hardware.bydauto.setting.BYDAutoSettingDevice;
import android.hardware.bydauto.speed.AbsBYDAutoSpeedListener;
import android.hardware.bydauto.speed.BYDAutoSpeedDevice;
import android.hardware.bydauto.statistic.AbsBYDAutoStatisticListener;
import android.hardware.bydauto.statistic.BYDAutoStatisticDevice;
import android.hardware.bydauto.tyre.AbsBYDAutoTyreListener;
import android.hardware.bydauto.tyre.BYDAutoTyreDevice;

public class DataListener {

    BYDAutoAcDevice autoAcDevice;
    BYDAutoBodyworkDevice autoBodyworkDevice;
    BYDAutoChargingDevice autoChargingDevice;
    BYDAutoDoorLockDevice autoDoorLockDevice;
    BYDAutoEnergyDevice autoEnergyDevice;
    BYDAutoEngineDevice autoEngineDevice;
    BYDAutoGearboxDevice autoGearboxDevice;
    BYDAutoInstrumentDevice autoInstrumentDevice;
    //    BYDAutoLightDevice autoLightDevice;
    //            BYDAutoMultimediaDevice autoMultimediaDevice;
    BYDAutoPanoramaDevice autoPanoramaDevice;
    //            BYDAutoPM2p5Device autoPM2p5Device;
    BYDAutoRadarDevice autoRadarDevice;
    //            BYDAutoSafetyBeltDevice autoSafetyBeltDevice;
    BYDAutoSensorDevice autoSensorDevice;
    BYDAutoSettingDevice autoSettingDevice;
    BYDAutoSpeedDevice autoSpeedDevice;
    BYDAutoStatisticDevice autoStatisticDevice;
    //    BYDAutoTimeDevice autoTimeDevice;
    BYDAutoTyreDevice autoTyreDevice;
    LocationTracker locationTracker;

    Context mContext;
    public DataListener(Context context) {
        mContext = context;




        autoAcDevice = BYDAutoAcDevice.getInstance(mContext);
        autoBodyworkDevice = BYDAutoBodyworkDevice.getInstance(mContext);
        autoChargingDevice = BYDAutoChargingDevice.getInstance(mContext);
        autoDoorLockDevice = BYDAutoDoorLockDevice.getInstance(mContext);
        autoEnergyDevice = BYDAutoEnergyDevice.getInstance(mContext);
        autoEngineDevice = BYDAutoEngineDevice.getInstance(mContext);
        autoGearboxDevice = BYDAutoGearboxDevice.getInstance(mContext);
        autoInstrumentDevice = BYDAutoInstrumentDevice.getInstance(mContext);
        autoPanoramaDevice = BYDAutoPanoramaDevice.getInstance(mContext);
        autoRadarDevice = BYDAutoRadarDevice.getInstance(mContext);
        autoSensorDevice = BYDAutoSensorDevice.getInstance(mContext);
        autoSettingDevice = BYDAutoSettingDevice.getInstance(mContext);
        autoSpeedDevice = BYDAutoSpeedDevice.getInstance(mContext);
        autoStatisticDevice = BYDAutoStatisticDevice.getInstance(mContext);
        autoTyreDevice = BYDAutoTyreDevice.getInstance(mContext);

        autoAcDevice.registerListener(acListener);
        autoBodyworkDevice.registerListener(bodyworkListener);
        autoChargingDevice.registerListener(chargingListener);
        autoDoorLockDevice.registerListener(doorLockListener);
        autoEnergyDevice.registerListener(energyListener);
        autoEngineDevice.registerListener(engineListener);
        autoGearboxDevice.registerListener(gearboxListener);
        autoInstrumentDevice.registerListener(instrumentListener);
        autoPanoramaDevice.registerListener(panoramaListener);
        autoRadarDevice.registerListener(radarListener);
        autoSensorDevice.registerListener(sensorListener);
        autoSettingDevice.registerListener(settingListener);
        autoSpeedDevice.registerListener(speedListener);
        autoStatisticDevice.registerListener(statisticListener);
        autoTyreDevice.registerListener(tyreListener);

        if (!EnvironmentUtils.isEmulator()) {
            fetchFristValueForEachKey();
        }

        String VIN = DataHolder.get("VIN");
        if (VIN == null || VIN.isEmpty()) {
            VIN = "SAMPLE_VIN_XX112233";
            DataHolder.put("VIN", VIN);
        }

        processer = new DataProcesser(mContext); // Pass context to DataProcesser

        locationTracker = new LocationTracker(mContext, processer);
        locationTracker.startTracking();

        if (EnvironmentUtils.isEmulator()) {
            new Thread(new Runnable() {
                @Override
                public void run() {

                    while (true) {
                        StringBuilder dataBuilder = new StringBuilder();
                        for (int i = 0; i < 10; i++) {
                            dataBuilder.append((char) ('A' + (int) (Math.random() * 26)));
                        }
                        String data = dataBuilder.toString();
                        processer.put("fakedata", "online", data);
                        try {
                            Thread.sleep(1500);
                        } catch (Exception e) {}
                    }

                }
            }).start();
        }
    }

    DataProcesser processer;

    void fetchFristValueForEachKey() {
        // AC Listener
//        int acWindModeShownState = autoAcDevice.getAcWindModeShownState();
//        processer.put("ac", "AcWindModeShownState", String.valueOf(acWindModeShownState));

        String autoVIN = autoBodyworkDevice.getAutoVIN();
        processer.put("bodywork", "AutoVIN", autoVIN);

        DataHolder.getInstance().put("VIN", autoVIN);

        int temperature = autoAcDevice.getTemprature(BYDAutoAcDevice.AC_TEMPERATURE_MAIN);
        processer.put("ac", "Temperature", String.valueOf(temperature));

        int temperatureUnit = autoAcDevice.getTemperatureUnit();
        processer.put("ac", "TemperatureUnit", String.valueOf(temperatureUnit));

        int acWindLevel = autoAcDevice.getAcWindLevel();
        processer.put("ac", "AcWindLevel", String.valueOf(acWindLevel));

        int acWindLevelManualSign = autoAcDevice.getAcWindLevelManualSign();
        processer.put("ac", "AcWindLevelManualSign", String.valueOf(acWindLevelManualSign));

        int acWindMode = autoAcDevice.getAcWindMode();
        processer.put("ac", "AcWindMode", String.valueOf(acWindMode));

        int acWindModeManualSign = autoAcDevice.getAcWindModeManualSign();
        processer.put("ac", "AcWindModeManualSign", String.valueOf(acWindModeManualSign));

        int acCompressorMode = autoAcDevice.getAcCompressorMode();
        processer.put("ac", "AcCompressorMode", String.valueOf(acCompressorMode));

        int acCompressorManualSign = autoAcDevice.getAcCompressorManualSign();
        processer.put("ac", "AcCompressorManualSign", String.valueOf(acCompressorManualSign));

        int acDefrostState = autoAcDevice.getAcDefrostState(BYDAutoAcDevice.AC_DEFROST_AREA_FRONT);
        processer.put("ac", "AcDefrostState", String.valueOf(acDefrostState));

        int acVentilationState = autoAcDevice.getAcVentilationState();
        processer.put("ac", "AcVentilationState", String.valueOf(acVentilationState));

        int acCycleMode = autoAcDevice.getAcCycleMode();
        processer.put("ac", "AcCycleMode", String.valueOf(acCycleMode));

        int acCtrlMode = autoAcDevice.getAcControlMode();
        processer.put("ac", "AcCtrlMode", String.valueOf(acCtrlMode));

        // Bodywork Listener
        int windowState = autoBodyworkDevice.getWindowState(BYDAutoBodyworkDevice.	BODYWORK_CMD_WINDOW_LEFT_FRONT);
        processer.put("bodywork", "WindowState", String.valueOf(windowState));

        int doorState = autoBodyworkDevice.getDoorState(BYDAutoBodyworkDevice.BODYWORK_CMD_DOOR_LEFT_FRONT);
        processer.put("bodywork", "DoorState", String.valueOf(doorState));

        int autoSystemState = autoBodyworkDevice.getAutoSystemState();
        processer.put("bodywork", "AutoSystemState", String.valueOf(autoSystemState));

        double steeringWheelValue = autoBodyworkDevice.getSteeringWheelValue(BYDAutoBodyworkDevice.BODYWORK_CMD_STEERING_WHEEL_ANGEL);
        processer.put("bodywork", "SteeringWheelValue", String.valueOf(steeringWheelValue));

//        double steeringWheelAngle = autoBodyworkDevice.getSteeringWheelAngle();
//        processer.put("bodywork", "SteeringWheelAngle", String.valueOf(steeringWheelAngle));

//        double steeringWheelSpeed = autoBodyworkDevice.getSteeringWheelSpeed();
//        processer.put("bodywork", "SteeringWheelSpeed", String.valueOf(steeringWheelSpeed));

        int powerLevel = autoBodyworkDevice.getPowerLevel();
        processer.put("bodywork", "PowerLevel", String.valueOf(powerLevel));

        int batteryVoltageLevel = autoBodyworkDevice.getBatteryVoltageLevel();
        processer.put("bodywork", "BatteryVoltageLevel", String.valueOf(batteryVoltageLevel));

//        int powerDayMode = autoBodyworkDevice.getPowerDayMode();
//        processer.put("bodywork", "PowerDayMode", String.valueOf(powerDayMode));

        int moonRoofConfig = autoBodyworkDevice.getMoonRoofConfig();
        processer.put("bodywork", "MoonRoofConfig", String.valueOf(moonRoofConfig));

        int fuelElecLowPower = autoBodyworkDevice.getFuelElecLowPower();
        processer.put("bodywork", "FuelElecLowPower", String.valueOf(fuelElecLowPower));

        int alarmState = autoBodyworkDevice.getAlarmState();
        processer.put("bodywork", "AlarmState", String.valueOf(alarmState));

        int windowOpenPercent = autoBodyworkDevice.getWindowOpenPercent(BYDAutoBodyworkDevice.BODYWORK_CMD_WINDOW_LEFT_FRONT);
        processer.put("bodywork", "WindowOpenPercent", String.valueOf(windowOpenPercent));

//        int carWindowAntiPinchConfig = autoBodyworkDevice.getCarWindowAntiPinchConfig();
//        processer.put("bodywork", "CarWindowAntiPinchConfig", String.valueOf(carWindowAntiPinchConfig));

//        int rainCloseWindow = autoBodyworkDevice.getRainCloseWindow();
//        processer.put("bodywork", "RainCloseWindow", String.valueOf(rainCloseWindow));

//        int message5sOnlineState = autoBodyworkDevice.getMessage5sOnlineState(0); // Assuming 0 for id
//        processer.put("bodywork", "Message5sOnlineState", String.valueOf(message5sOnlineState));

//        int hasMessage = autoBodyworkDevice.getHasMessage(0); // Assuming 0 for id
//        processer.put("bodywork", "HasMessage", String.valueOf(hasMessage));

//        int sunroofState = autoBodyworkDevice.getSunroofState();
//        processer.put("bodywork", "SunroofState", String.valueOf(sunroofState));

//        int windowPermitState = autoBodyworkDevice.getWindowPermitState();
//        processer.put("bodywork", "WindowPermitState", String.valueOf(windowPermitState));

//        int windoblindInitState = autoBodyworkDevice.getWindoblindInitState();
//        processer.put("bodywork", "WindoblindInitState", String.valueOf(windoblindInitState));

//        int sunroofInitState = autoBodyworkDevice.getSunroofInitState();
//        processer.put("bodywork", "SunroofInitState", String.valueOf(sunroofInitState));

//        int sunroofCloseNotice = autoBodyworkDevice.getSunroofCloseNotice();
//        processer.put("bodywork", "SunroofCloseNotice", String.valueOf(sunroofCloseNotice));

//        int sunroofWindowblindPosition = autoBodyworkDevice.getSunroofWindowblindPosition();
//        processer.put("bodywork", "SunroofWindowblindPosition", String.valueOf(sunroofWindowblindPosition));

//        int sunroofPosition = autoBodyworkDevice.getSunroofPosition();
//        processer.put("bodywork", "SunroofPosition", String.valueOf(sunroofPosition));

//        int smartVoiceLimit = autoBodyworkDevice.getSmartVoiceLimit();
//        processer.put("bodywork", "SmartVoiceLimit", String.valueOf(smartVoiceLimit));

//        int batteryPower = autoBodyworkDevice.getBatteryPower();
//        processer.put("bodywork", "BatteryPower", String.valueOf(batteryPower));

        // Charging Listener
        int chargerFaultState = autoChargingDevice.getChargerFaultState();
        processer.put("charging", "ChargerFaultState", String.valueOf(chargerFaultState));

        int chargerWorkState = autoChargingDevice.getChargerWorkState();
        processer.put("charging", "ChargerWorkState", String.valueOf(chargerWorkState));

        double chargingCapacity = autoChargingDevice.getChargingCapacity();
        processer.put("charging", "ChargingCapacity", String.valueOf(chargingCapacity));

        int chargingType = autoChargingDevice.getChargingType();
        processer.put("charging", "ChargingType", String.valueOf(chargingType));

        int[] chargingRestTime = autoChargingDevice.getChargingRestTime();
        processer.put("charging", "ChargingRestTime", Arrays.toString(chargingRestTime));

//        int chargingCapState = autoChargingDevice.getChargingCapState(BYDAutoChargingDevice.CHARGE_CAP_STATE_AC);
//        processer.put("charging", "ChargingCapState", String.valueOf(chargingCapState));

        int chargingPortLockRebackState = autoChargingDevice.getChargingPortLockRebackState();
        processer.put("charging", "ChargingPortLockRebackState", String.valueOf(chargingPortLockRebackState));

        int dischargeRequestState = autoChargingDevice.getDischargeRequestState();
        processer.put("charging", "DischargeRequestState", String.valueOf(dischargeRequestState));

        int chargerState = autoChargingDevice.getChargerState();
        processer.put("charging", "ChargerState", String.valueOf(chargerState));

        int chargingGunState = autoChargingDevice.getChargingGunState();
        processer.put("charging", "ChargingGunState", String.valueOf(chargingGunState));

        double chargingPower = autoChargingDevice.getChargingPower();
        processer.put("charging", "ChargingPower", String.valueOf(chargingPower));

        int batteryManagementDeviceState = autoChargingDevice.getBatteryManagementDeviceState();
        processer.put("charging", "BatteryManagementDeviceState", String.valueOf(batteryManagementDeviceState));

        int chargingScheduleEnableState = autoChargingDevice.getChargingScheduleEnableState();
        processer.put("charging", "ChargingScheduleEnableState", String.valueOf(chargingScheduleEnableState));

        int chargingScheduleState = autoChargingDevice.getChargingScheduleState();
        processer.put("charging", "ChargingScheduleState", String.valueOf(chargingScheduleState));

        int chargingGunNotInsertedState = autoChargingDevice.getChargingGunNotInsertedState();
        processer.put("charging", "ChargingGunNotInsertedState", String.valueOf(chargingGunNotInsertedState));

        int[] chargingScheduleTime = autoChargingDevice.getChargingScheduleTime();
        processer.put("charging", "ChargingScheduleTime", Arrays.toString(chargingScheduleTime));

        // NoSuchMethodError
//        int chargingState = autoChargingDevice.getChargingState();
//        processer.put("charging", "ChargingState", String.valueOf(chargingState));

        // NoSuchMethodError
//        int chargingMode = autoChargingDevice.getChargingMode();
//        processer.put("charging", "ChargingMode", String.valueOf(chargingMode));
        // Check if the device is an emulator


        if (!EnvironmentUtils.isEmulator()) {
            ChargingTimerInfo chargingTimerInfo = autoChargingDevice.getChargingTimerInfo(0);
            processer.put("charging", "ChargingTimerInfo", String.valueOf(chargingTimerInfo));
        }


        int wirelessChargingSwitchState = autoChargingDevice.getWirelessChargingSwitchState();
        processer.put("charging", "WirelessChargingSwitchState", String.valueOf(wirelessChargingSwitchState));

        int wirelessChargingOnline5sState = autoChargingDevice.getWirelessChargingOnline5sState();
        processer.put("charging", "WirelessChargingOnline5sState", String.valueOf(wirelessChargingOnline5sState));

        int smartChargingState = autoChargingDevice.getSmartChargingState();
        processer.put("charging", "SmartChargingState", String.valueOf(smartChargingState));

//        int dischargeState = autoChargingDevice.getDischargeState(BYDAutoChargingDevice.DISCHARGE_STATE_V2L);
//        processer.put("charging", "DischargeState", String.valueOf(dischargeState));

//        int disChargeWarningState = autoChargingDevice.getDisChargeWarningState();
//        processer.put("charging", "DisChargeWarningState", String.valueOf(disChargeWarningState));

//        int wirlessChargingState = autoChargingDevice.getWirlessChargingState();
//        processer.put("charging", "WirlessChargingState", String.valueOf(wirlessChargingState));

        int chargeTempCtlState = autoChargingDevice.getChargeTempCtlState();
        processer.put("charging", "ChargeTempCtlState", String.valueOf(chargeTempCtlState));

        int batteryType = autoChargingDevice.getBatteryType();
        processer.put("charging", "BatteryType", String.valueOf(batteryType));

        int chargeStopSwitchState = autoChargingDevice.getChargeStopSwitchState();
        processer.put("charging", "ChargeStopSwitchState", String.valueOf(chargeStopSwitchState));

        int chargeStopCapacityState = autoChargingDevice.getChargeStopCapacityState();
        processer.put("charging", "ChargeStopCapacityState", String.valueOf(chargeStopCapacityState));

        int weatherAndTimeRequest = autoChargingDevice.getWeatherAndTimeRequest();
        processer.put("charging", "WeatherAndTimeRequest", String.valueOf(weatherAndTimeRequest));

        int carDischargeState = autoChargingDevice.getCarDischargeState();
        processer.put("charging", "CarDischargeState", String.valueOf(carDischargeState));

        int carDischargeLowWarn = autoChargingDevice.getCarDischargeLowWarn();
        processer.put("charging", "CarDischargeLowWarn", String.valueOf(carDischargeLowWarn));

        int vtovDischargeConnectState = autoChargingDevice.getVtovDischargeConnectState();
        processer.put("charging", "VtovDischargeConnectState", String.valueOf(vtovDischargeConnectState));

        int vtovDischargeLimitVal = autoChargingDevice.getVtovDischargeLimitVal();
        processer.put("charging", "VtovDischargeLimitVal", String.valueOf(vtovDischargeLimitVal));

        int vtovDischargeLowestVal = autoChargingDevice.getVtovDischargeLowestVal();
        processer.put("charging", "VtovDischargeLowestVal", String.valueOf(vtovDischargeLowestVal));

        double vtovDischargeQuantity = autoChargingDevice.getVtovDischargeQuantity();
        processer.put("charging", "VtovDischargeQuantity", String.valueOf(vtovDischargeQuantity));

        int capState = autoChargingDevice.getCapState();
        processer.put("charging", "CapState", String.valueOf(capState));

        int socSaveSwitch = autoChargingDevice.getSocSaveSwitch();
        processer.put("charging", "SocSaveSwitch", String.valueOf(socSaveSwitch));

        // Door Lock Listener
        int doorLockStatus = autoDoorLockDevice.getDoorLockStatus(BYDAutoDoorLockDevice.DOOR_LOCK_AREA_LEFT_FRONT);
        processer.put("doorlock", "DoorLockStatus", String.valueOf(doorLockStatus));

        // Energy Listener
        int energyMode = autoEnergyDevice.getEnergyMode();
        processer.put("energy", "EnergyMode", String.valueOf(energyMode));

        int operationMode = autoEnergyDevice.getOperationMode();
        processer.put("energy", "OperationMode", String.valueOf(operationMode));

        int powerGenerationState = autoEnergyDevice.getPowerGenerationState();
        processer.put("energy", "PowerGenerationState", String.valueOf(powerGenerationState));

        int powerGenerationValue = autoEnergyDevice.getPowerGenerationValue();
        processer.put("energy", "PowerGenerationValue", String.valueOf(powerGenerationValue));

        int roadSurface = autoEnergyDevice.getRoadSurfaceMode();
        processer.put("energy", "RoadSurface", String.valueOf(roadSurface));

        // Engine Listener
        int engineSpeed = autoEngineDevice.getEngineSpeed();
        processer.put("engine", "EngineSpeed", String.valueOf(engineSpeed));

        int engineCoolantLevel = autoEngineDevice.getEngineCoolantLevel();
        processer.put("engine", "EngineCoolantLevel", String.valueOf(engineCoolantLevel));

        int oilLevel = autoEngineDevice.getOilLevel();
        processer.put("engine", "OilLevel", String.valueOf(oilLevel));

        // Gearbox Listener
        int gearboxAutoModeType = autoGearboxDevice.getGearboxAutoModeType();
        processer.put("gearbox", "GearboxAutoModeType", String.valueOf(gearboxAutoModeType));

        int gearboxManualModeLevel = autoGearboxDevice.getGearboxManualModeLevel();
        processer.put("gearbox", "GearboxManualModeLevel", String.valueOf(gearboxManualModeLevel));

        int brakeFluidLevel = autoGearboxDevice.getBrakeFluidLevel();
        processer.put("gearbox", "BrakeFluidLevel", String.valueOf(brakeFluidLevel));

        int parkBrakeSwitch = autoGearboxDevice.getParkBrakeSwitch();
        processer.put("gearbox", "ParkBrakeSwitch", String.valueOf(parkBrakeSwitch));

        int brakePedalState = autoGearboxDevice.getBrakePedalState();
        processer.put("gearbox", "BrakePedalState", String.valueOf(brakePedalState));

        // Instrument Listener
        int malfunctionInfo = autoInstrumentDevice.getMalfunctionInfo(0); // Assuming 0 for typeName
        processer.put("instrument", "MalfunctionInfo", String.valueOf(malfunctionInfo));

//        int malfunctionInfo2 = autoInstrumentDevice.getMalfunctionInfo2(0); // Assuming 0 for typeName
//        processer.put("instrument", "MalfunctionInfo2", String.valueOf(malfunctionInfo2));

//        int backlightModeState = autoInstrumentDevice.getBacklightModeState(0); // Assuming 0 for backlightMode
//        processer.put("instrument", "BacklightModeState", String.valueOf(backlightModeState));

//        int backlightBrightness = autoInstrumentDevice.getBacklightBrightness();
//        processer.put("instrument", "BacklightBrightness", String.valueOf(backlightBrightness));

        int unit = autoInstrumentDevice.getUnit(0); // Assuming 0 for unitName
        processer.put("instrument", "Unit", String.valueOf(unit));

        int maintenanceInfo = autoInstrumentDevice.getMaintenanceInfo(0); // Assuming 0 for typeName
        processer.put("instrument", "MaintenanceInfo", String.valueOf(maintenanceInfo));

//        int musicInfoResult = autoInstrumentDevice.getMusicInfoResult();
//        processer.put("instrument", "MusicInfoResult", String.valueOf(musicInfoResult));

//        int callInfoResult = autoInstrumentDevice.getCallInfoResult();
//        processer.put("instrument", "CallInfoResult", String.valueOf(callInfoResult));

//        int radioInfoResult = autoInstrumentDevice.getRadioInfoResult();
//        processer.put("instrument", "RadioInfoResult", String.valueOf(radioInfoResult));

        int alarmBuzzleState = autoInstrumentDevice.getAlarmBuzzleState();
        processer.put("instrument", "AlarmBuzzleState", String.valueOf(alarmBuzzleState));

//        int powerOnErrInfo = autoInstrumentDevice.getPowerOnErrInfo();
//        processer.put("instrument", "PowerOnErrInfo", String.valueOf(powerOnErrInfo));

//        int powerOffErrInfo = autoInstrumentDevice.getPowerOffErrInfo();
//        processer.put("instrument", "PowerOffErrInfo", String.valueOf(powerOffErrInfo));

//        int remoteDrivingReminder = autoInstrumentDevice.getRemoteDrivingReminder();
//        processer.put("instrument", "RemoteDrivingReminder", String.valueOf(remoteDrivingReminder));

//        int keyDetectionReminder = autoInstrumentDevice.getKeyDetectionReminder();
//        processer.put("instrument", "KeyDetectionReminder", String.valueOf(keyDetectionReminder));

//        int averageSpeed = autoInstrumentDevice.getAverageSpeed();
//        processer.put("instrument", "AverageSpeed", String.valueOf(averageSpeed));

        double externalChargingPower = autoInstrumentDevice.getExternalChargingPower();
        processer.put("instrument", "ExternalChargingPower", String.valueOf(externalChargingPower));

//        int instrumentScreenType = autoInstrumentDevice.getInstrumentScreenType();
//        processer.put("instrument", "InstrumentScreenType", String.valueOf(instrumentScreenType));

//        int naviDestinationCommand = autoInstrumentDevice.getNaviDestinationCommand();
//        processer.put("instrument", "NaviDestinationCommand", String.valueOf(naviDestinationCommand));

//        int roadNameCheckState = autoInstrumentDevice.getRoadNameCheckState();
//        processer.put("instrument", "RoadNameCheckState", String.valueOf(roadNameCheckState));

//        int textInfo = autoInstrumentDevice.getTextInfo();
//        processer.put("instrument", "TextInfo", String.valueOf(textInfo));

//        int moduleState = autoInstrumentDevice.getModuleState(0); // Assuming 0 for module
//        processer.put("instrument", "ModuleState", String.valueOf(moduleState));

//        int doorState = autoInstrumentDevice.getDoorState(0); // Assuming 0 for area
//        processer.put("instrument", "DoorState", String.valueOf(doorState));

//        int safetyBeltStatus = autoInstrumentDevice.getSafetyBeltStatus(0); // Assuming 0 for area
//        processer.put("instrument", "SafetyBeltStatus", String.valueOf(safetyBeltStatus));

//        int wheelColor = autoInstrumentDevice.getWheelColor(0); // Assuming 0 for position
//        processer.put("instrument", "WheelColor", String.valueOf(wheelColor));

//        int wheelTemperatureColor = autoInstrumentDevice.getWheelTemperatureColor(0); // Assuming 0 for position
//        processer.put("instrument", "WheelTemperatureColor", String.valueOf(wheelTemperatureColor));

//        int wheelPressure = autoInstrumentDevice.getWheelPressure(0); // Assuming 0 for position
//        processer.put("instrument", "WheelPressure", String.valueOf(wheelPressure));

//        int wheelTemperature = autoInstrumentDevice.getWheelTemperature(0); // Assuming 0 for position
//        processer.put("instrument", "WheelTemperature", String.valueOf(wheelTemperature));

//        int deviationState = autoInstrumentDevice.getDeviationState();
//        processer.put("instrument", "DeviationState", String.valueOf(deviationState));

//        int gapDetection = autoInstrumentDevice.getGapDetection();
//        processer.put("instrument", "GapDetection", String.valueOf(gapDetection));

//        int laneLineState = autoInstrumentDevice.getLaneLineState();
//        processer.put("instrument", "LaneLineState", String.valueOf(laneLineState));

//        int timeIntervalState = autoInstrumentDevice.getTimeIntervalState();
//        processer.put("instrument", "TimeIntervalState", String.valueOf(timeIntervalState));

//        int energyFeedback = autoInstrumentDevice.getEnergyFeedback();
//        processer.put("instrument", "EnergyFeedback", String.valueOf(energyFeedback));

//        int textColor = autoInstrumentDevice.getTextColor();
//        processer.put("instrument", "TextColor", String.valueOf(textColor));

//        int spacingState = autoInstrumentDevice.getSpacingState();
//        processer.put("instrument", "SpacingState", String.valueOf(spacingState));

//        int soundType = autoInstrumentDevice.getSoundType();
//        processer.put("instrument", "SoundType", String.valueOf(soundType));

//        int accCruisingSpeed = autoInstrumentDevice.getAccCruisingSpeed();
//        processer.put("instrument", "AccCruisingSpeed", String.valueOf(accCruisingSpeed));

//        int accCruisingSpeedColor = autoInstrumentDevice.getAccCruisingSpeedColor();
//        processer.put("instrument", "AccCruisingSpeedColor", String.valueOf(accCruisingSpeedColor));

//        int pcwAlarmInstruction = autoInstrumentDevice.getPCWAlarmInstruction();
//        processer.put("instrument", "PCWAlarmInstruction", String.valueOf(pcwAlarmInstruction));

//        int laneLineColor = autoInstrumentDevice.getLaneLineColor();
//        processer.put("instrument", "LaneLineColor", String.valueOf(laneLineColor));

//        int totalMileage = autoInstrumentDevice.getTotalMileage();
//        processer.put("instrument", "TotalMileage", String.valueOf(totalMileage));

//        int mileageUnit = autoInstrumentDevice.getMileageUnit();
//        processer.put("instrument", "MileageUnit", String.valueOf(mileageUnit));

//        double last50KmPowerConsume = autoInstrumentDevice.getLast50KmPowerConsume();
//        processer.put("instrument", "Last50KmPowerConsume", String.valueOf(last50KmPowerConsume));

//        int speedUnit = autoInstrumentDevice.getSpeedUnit();
//        processer.put("instrument", "SpeedUnit", String.valueOf(speedUnit));

//        int batteryPercent = autoInstrumentDevice.getBatteryPercent();
//        processer.put("instrument", "BatteryPercent", String.valueOf(batteryPercent));

//        double externalChargePower = autoInstrumentDevice.getExternalChargePower();
//        processer.put("instrument", "ExternalChargePower", String.valueOf(externalChargePower));

//        double travelTime = autoInstrumentDevice.getTravelTime();
//        processer.put("instrument", "TravelTime", String.valueOf(travelTime));

//        int powerUnit = autoInstrumentDevice.getPowerUnit();
//        processer.put("instrument", "PowerUnit", String.valueOf(powerUnit));

//        int airHeatOilDisplay = autoInstrumentDevice.getAirHeatOilDisplay();
//        processer.put("instrument", "AirHeatOilDisplay", String.valueOf(airHeatOilDisplay));

//        int chargeDisplay = autoInstrumentDevice.getChargeDisplay();
//        processer.put("instrument", "ChargeDisplay", String.valueOf(chargeDisplay));

//        int chargePercent = autoInstrumentDevice.getChargePercent();
//        processer.put("instrument", "ChargePercent", String.valueOf(chargePercent));

//        double chargePower = autoInstrumentDevice.getChargePower();
//        processer.put("instrument", "ChargePower", String.valueOf(chargePower));

//        int chargeNotice = autoInstrumentDevice.getChargeNotice();
//        processer.put("instrument", "ChargeNotice", String.valueOf(chargeNotice));

//        int[] chargeRestTime = autoInstrumentDevice.getChargeRestTime();
//        processer.put("instrument", "ChargeRestTime", Arrays.toString(chargeRestTime));

//        int expectChargeState = autoInstrumentDevice.getExpectChargeState();
//        processer.put("instrument", "ExpectChargeState", String.valueOf(expectChargeState));

//        int expectChargeDisplay = autoInstrumentDevice.getExpectChargeDisplay();
//        processer.put("instrument", "ExpectChargeDisplay", String.valueOf(expectChargeDisplay));

//        int accIndicateLightState = autoInstrumentDevice.getACCIndicateLightState();
//        processer.put("instrument", "ACCIndicateLightState", String.valueOf(accIndicateLightState));

//        int accIndicateLightColor = autoInstrumentDevice.getACCIndicateLightColor();
//        processer.put("instrument", "ACCIndicateLightColor", String.valueOf(accIndicateLightColor));

//        int accCruisingSpeedValue = autoInstrumentDevice.getAccCruisingSpeedValue();
//        processer.put("instrument", "AccCruisingSpeedValue", String.valueOf(accCruisingSpeedValue));

//        int oilLevelAlarmIndicator = autoInstrumentDevice.getOilLevelAlarmIndicator();
//        processer.put("instrument", "OilLevelAlarmIndicator", String.valueOf(oilLevelAlarmIndicator));

//        int oilLevelAlarmIndicatorColor = autoInstrumentDevice.getOilLevelAlarmIndicatorColor();
//        processer.put("instrument", "OilLevelAlarmIndicatorColor", String.valueOf(oilLevelAlarmIndicatorColor));

//        int outCarTemperature = autoInstrumentDevice.getOutCarTemperature();
//        processer.put("instrument", "OutCarTemperature", String.valueOf(outCarTemperature));

//        int linkErrKeyTime = autoInstrumentDevice.getLinkErrKeyTime(0); // Assuming 0 for flag
//        processer.put("instrument", "LinkErrKeyTime", String.valueOf(linkErrKeyTime));

//        int srsFaultWarningLight = autoInstrumentDevice.getSRSFaultWarningLight();
//        processer.put("instrument", "SRSFaultWarningLight", String.valueOf(srsFaultWarningLight));

//        int srsFaultWarningLightColor = autoInstrumentDevice.getSRSFaultWarningLightColor();
//        processer.put("instrument", "SRSFaultWarningLightColor", String.valueOf(srsFaultWarningLightColor));

//        int absFaultWarningLight = autoInstrumentDevice.getABSFaultWarningLight();
//        processer.put("instrument", "ABSFaultWarningLight", String.valueOf(absFaultWarningLight));

//        int absFaultWarningLightColor = autoInstrumentDevice.getABSFaultWarningLightColor();
//        processer.put("instrument", "ABSFaultWarningLightColor", String.valueOf(absFaultWarningLightColor));

//        int brakeSysFaultLightState = autoInstrumentDevice.getBrakeSysFaultLightState();
//        processer.put("instrument", "BrakeSysFaultLightState", String.valueOf(brakeSysFaultLightState));

//        int brakeSysFaultLightColor = autoInstrumentDevice.getBrakeSysFaultLightColor();
//        processer.put("instrument", "BrakeSysFaultLightColor", String.valueOf(brakeSysFaultLightColor));

//        int coolantTempHighWarnLightState = autoInstrumentDevice.getCoolantTempHighWarnLightState();
//        processer.put("instrument", "CoolantTempHighWarnLightState", String.valueOf(coolantTempHighWarnLightState));

//        int coolantTempHighWarnLightColor = autoInstrumentDevice.getCoolantTempHighWarnLightColor();
//        processer.put("instrument", "CoolantTempHighWarnLightColor", String.valueOf(coolantTempHighWarnLightColor));

//        int elecPakingState = autoInstrumentDevice.getELECParkingState();
//        processer.put("instrument", "ELECParkingState", String.valueOf(elecPakingState));

//        int elecPakingColor = autoInstrumentDevice.getELECParkingColor();
//        processer.put("instrument", "ELECParkingColor", String.valueOf(elecPakingColor));

//        int engineFailWarnLightState = autoInstrumentDevice.getEngineFailWarnLightState();
//        processer.put("instrument", "EngineFailWarnLightState", String.valueOf(engineFailWarnLightState));

//        int engineFailWarnLightColor = autoInstrumentDevice.getEngineFailWarnLightColor();
//        processer.put("instrument", "EngineFailWarnLightColor", String.valueOf(engineFailWarnLightColor));

//        int espFailWarnLightState = autoInstrumentDevice.getESPFailWarnLightState();
//        processer.put("instrument", "ESPFailWarnLightState", String.valueOf(espFailWarnLightState));

//        int espFailWarnLightColor = autoInstrumentDevice.getESPFailWarnLightColor();
//        processer.put("instrument", "ESPFailWarnLightColor", String.valueOf(espFailWarnLightColor));

//        int gpfIndicatorState = autoInstrumentDevice.getGPFIndicatorState();
//        processer.put("instrument", "GPFIndicatorState", String.valueOf(gpfIndicatorState));

//        int gpfIndicatorColor = autoInstrumentDevice.getGPFIndicatorColor();
//        processer.put("instrument", "GPFIndicatorColor", String.valueOf(gpfIndicatorColor));

//        int lowFuelWarnLightState = autoInstrumentDevice.getLowFuelWarnLightState();
//        processer.put("instrument", "LowFuelWarnLightState", String.valueOf(lowFuelWarnLightState));

//        int lowFuelWarnLightColor = autoInstrumentDevice.getLowFuelWarnLightColor();
//        processer.put("instrument", "LowFuelWarnLightColor", String.valueOf(lowFuelWarnLightColor));

//        int pressureWarnLightState = autoInstrumentDevice.getPressureWarnLightState();
//        processer.put("instrument", "PressureWarnLightState", String.valueOf(pressureWarnLightState));

//        int pressureWarnLightColor = autoInstrumentDevice.getPressureWarnLightColor();
//        processer.put("instrument", "PressureWarnLightColor", String.valueOf(pressureWarnLightColor));

//        int mainAlarmIndicatorState = autoInstrumentDevice.getMainAlarmIndicatorState();
//        processer.put("instrument", "MainAlarmIndicatorState", String.valueOf(mainAlarmIndicatorState));

//        int mainAlarmIndicatorColor = autoInstrumentDevice.getMainAlarmIndicatorColor();
//        processer.put("instrument", "MainAlarmIndicatorColor", String.valueOf(mainAlarmIndicatorColor));

//        int pressureSupplySysFailWarnLightState = autoInstrumentDevice.getPressureSupplySysFailWarnLightState();
//        processer.put("instrument", "PressureSupplySysFailWarnLightState", String.valueOf(pressureSupplySysFailWarnLightState));

//        int pressureSupplySysFailWarnLightColor = autoInstrumentDevice.getPressureSupplySysFailWarnLightColor();
//        processer.put("instrument", "PressureSupplySysFailWarnLightColor", String.valueOf(pressureSupplySysFailWarnLightColor));

//        int smartKeySysWarnLightState = autoInstrumentDevice.getSmartKeySysWarnLightState();
//        processer.put("instrument", "SmartKeySysWarnLightState", String.valueOf(smartKeySysWarnLightState));

//        int smartKeySysWarnLightColor = autoInstrumentDevice.getSmartKeySysWarnLightColor();
//        processer.put("instrument", "SmartKeySysWarnLightColor", String.valueOf(smartKeySysWarnLightColor));

//        int steeringSysFailWarnLightState = autoInstrumentDevice.getSteeringSYSFailWarnLightState();
//        processer.put("instrument", "SteeringSYSFailWarnLightState", String.valueOf(steeringSysFailWarnLightState));

//        int steeringSysFailWarnLightColor = autoInstrumentDevice.getSteeringSYSFailWarnLightColor();
//        processer.put("instrument", "SteeringSYSFailWarnLightColor", String.valueOf(steeringSysFailWarnLightColor));

//        int tyrePressureSysFailWarnLightState = autoInstrumentDevice.getTyrePressureSYSFailWarnLightState();
//        processer.put("instrument", "TyrePressureSYSFailWarnLightState", String.valueOf(tyrePressureSysFailWarnLightState));

//        int tyrePressureSysFailWarnLightColor = autoInstrumentDevice.getTyrePressureSYSFailWarnLightColor();
//        processer.put("instrument", "TyrePressureSYSFailWarnLightColor", String.valueOf(tyrePressureSysFailWarnLightColor));

//        int headlampFailWarnLightState = autoInstrumentDevice.getHeadlampFailWarnLightState();
//        processer.put("instrument", "HeadlampFailWarnLightState", String.valueOf(headlampFailWarnLightState));

//        int headlampFailWarnLightColor = autoInstrumentDevice.getHeadlampFailWarnLightColor();
//        processer.put("instrument", "HeadlampFailWarnLightColor", String.valueOf(headlampFailWarnLightColor));

//        int cruiseCtrlIndicatorState = autoInstrumentDevice.getCruiseCtrlIndicatorState();
//        processer.put("instrument", "CruiseCtrlIndicatorState", String.valueOf(cruiseCtrlIndicatorState));

//        int cruiseCtrlIndicatorColor = autoInstrumentDevice.getCruiseCtrlIndicatorColor();
//        processer.put("instrument", "CruiseCtrlIndicatorColor", String.valueOf(cruiseCtrlIndicatorColor));

//        int dishargeIndicatorState = autoInstrumentDevice.getDishargeIndicatorState();
//        processer.put("instrument", "DishargeIndicatorState", String.valueOf(dishargeIndicatorState));

//        int dischargeIndicatorColor = autoInstrumentDevice.getDischargeIndicatorColor();
//        processer.put("instrument", "DischargeIndicatorColor", String.valueOf(dischargeIndicatorColor));

//        int drivePowerLimitIndicatorState = autoInstrumentDevice.getDrivePowerLimitIndicatorState();
//        processer.put("instrument", "DrivePowerLimitIndicatorState", String.valueOf(drivePowerLimitIndicatorState));

//        int drivePowerLimitIndicatorColor = autoInstrumentDevice.getDrivePowerLimitIndicatorColor();
//        processer.put("instrument", "DrivePowerLimitIndicatorColor", String.valueOf(drivePowerLimitIndicatorColor));

//        int ecoIndicatorState = autoInstrumentDevice.getECOIndicatorState();
//        processer.put("instrument", "ECOIndicatorState", String.valueOf(ecoIndicatorState));

//        int ecoIndicatorColor = autoInstrumentDevice.getECOIndicatorColor();
//        processer.put("instrument", "ECOIndicatorColor", String.valueOf(ecoIndicatorColor));

//        int evIndicatorState = autoInstrumentDevice.getEVIndicatorState();
//        processer.put("instrument", "EVIndicatorState", String.valueOf(evIndicatorState));

//        int evIndicatorColor = autoInstrumentDevice.getEVIndicatorColor();
//        processer.put("instrument", "EVIndicatorColor", String.valueOf(evIndicatorColor));

//        int hevIndicatorState = autoInstrumentDevice.getHEVIndicatorState();
//        processer.put("instrument", "HEVIndicatorState", String.valueOf(hevIndicatorState));

//        int hevIndicatorColor = autoInstrumentDevice.getHEVIndicatorColor();
//        processer.put("instrument", "HEVIndicatorColor", String.valueOf(hevIndicatorColor));

//        int lowPowerBatteryWarnLightState = autoInstrumentDevice.getLowPowerBatteryWarnLightState();
//        processer.put("instrument", "LowPowerBatteryWarnLightState", String.valueOf(lowPowerBatteryWarnLightState));

//        int lowPowerBatteryWarnLightColor = autoInstrumentDevice.getLowPowerBatteryWarnLightColor();
//        processer.put("instrument", "LowPowerBatteryWarnLightColor", String.valueOf(lowPowerBatteryWarnLightColor));

//        int okIndicatorState = autoInstrumentDevice.getOKIndicatorState();
//        processer.put("instrument", "OKIndicatorState", String.valueOf(okIndicatorState));

//        int okIndicatorColor = autoInstrumentDevice.getOKIndicatorColor();
//        processer.put("instrument", "OKIndicatorColor", String.valueOf(okIndicatorColor));

//        int powerBatteryChargeConnectIndicatorState = autoInstrumentDevice.getPowerBatteryChargeConnectIndicatorState();
//        processer.put("instrument", "PowerBatteryChargeConnectIndicatorState", String.valueOf(powerBatteryChargeConnectIndicatorState));

//        int powerBatteryChargeConnectIndicatorColor = autoInstrumentDevice.getPowerBatteryChargeConnectIndicatorColor();
//        processer.put("instrument", "PowerBatteryChargeConnectIndicatorColor", String.valueOf(powerBatteryChargeConnectIndicatorColor));

//        int powerBatteryHeatWarnLightState = autoInstrumentDevice.getPowerBatteryHeatWarnLightState();
//        processer.put("instrument", "PowerBatteryHeatWarnLightState", String.valueOf(powerBatteryHeatWarnLightState));

//        int powerBatteryHeatWarnLightColor = autoInstrumentDevice.getPowerBatteryHeatWarnLightColor();
//        processer.put("instrument", "PowerBatteryHeatWarnLightColor", String.valueOf(powerBatteryHeatWarnLightColor));

//        int powerBatFailWarnLightState = autoInstrumentDevice.getPowerBatFailWarnLightState();
//        processer.put("instrument", "PowerBatFailWarnLightState", String.valueOf(powerBatFailWarnLightState));

//        int powerBatFailWarnLightColor = autoInstrumentDevice.getPowerBatFailWarnLightColor();
//        processer.put("instrument", "PowerBatFailWarnLightColor", String.valueOf(powerBatFailWarnLightColor));

//        int powerSysFailWarnLightState = autoInstrumentDevice.getPowerSysFailWarnLightState();
//        processer.put("instrument", "PowerSysFailWarnLightState", String.valueOf(powerSysFailWarnLightState));

//        int powerSysFailWarnLightColor = autoInstrumentDevice.getPowerSysFailWarnLightColor();
//        processer.put("instrument", "PowerSysFailWarnLightColor", String.valueOf(powerSysFailWarnLightColor));

//        int sportIndicatorState = autoInstrumentDevice.getSportIndicatorState();
//        processer.put("instrument", "SportIndicatorState", String.valueOf(sportIndicatorState));

//        int sportIndicatorColor = autoInstrumentDevice.getSportIndicatorColor();
//        processer.put("instrument", "SportIndicatorColor", String.valueOf(sportIndicatorColor));

//        int dischargeUiState = autoInstrumentDevice.getDischargeUiState();
//        processer.put("instrument", "DischargeUiState", String.valueOf(dischargeUiState));

//        int dischargeMode = autoInstrumentDevice.getDischargeMode();
//        processer.put("instrument", "DischargeMode", String.valueOf(dischargeMode));

//        double dischargeElecEnergy = autoInstrumentDevice.getDischargeElecEnergy();
//        processer.put("instrument", "DischargeElecEnergy", String.valueOf(dischargeElecEnergy));

//        int directionInfo = autoInstrumentDevice.getDirectionInfo();
//        processer.put("instrument", "DirectionInfo", String.valueOf(directionInfo));

//        int tyrePressureCarType = autoInstrumentDevice.getTyrePressureCarType();
//        processer.put("instrument", "TyrePressureCarType", String.valueOf(tyrePressureCarType));

//        int mileageValidFlag = autoInstrumentDevice.getMileageValidFlag();
//        processer.put("instrument", "MileageValidFlag", String.valueOf(mileageValidFlag));

//        int dashboardAlarmState = autoInstrumentDevice.getDashboardAlarmState();
//        processer.put("instrument", "DashboardAlarmState", String.valueOf(dashboardAlarmState));

//        double currentJourneyDriveMileage = autoInstrumentDevice.getCurrentJourneyDriveMileage();
//        processer.put("instrument", "CurrentJourneyDriveMileage", String.valueOf(currentJourneyDriveMileage));

//        double currentJourneyDriveTime = autoInstrumentDevice.getCurrentJourneyDriveTime();
//        processer.put("instrument", "CurrentJourneyDriveTime", String.valueOf(currentJourneyDriveTime));

//        int currentDriveInterFace = autoInstrumentDevice.getCurrentDriveInterFace();
//        processer.put("instrument", "CurrentDriveInterFace", String.valueOf(currentDriveInterFace));

//        int odometerDisplay = autoInstrumentDevice.getOdometerDisplay();
//        processer.put("instrument", "OdometerDisplay", String.valueOf(odometerDisplay));

//        int appCountdownHour = autoInstrumentDevice.getAppCountdownHour();
//        processer.put("instrument", "AppCountdownHour", String.valueOf(appCountdownHour));
//
//        int appCountdownMinute = autoInstrumentDevice.getAppCountdownMinute();
//        processer.put("instrument", "AppCountdownMinute", String.valueOf(appCountdownMinute));
//
//        int viewStatus = autoInstrumentDevice.getViewStatus();
//        processer.put("instrument", "ViewStatus", String.valueOf(viewStatus));
//
//        int firstMenu = autoInstrumentDevice.getFirstMenu();
//        processer.put("instrument", "FirstMenu", String.valueOf(firstMenu));
//
//        int secondMenu = autoInstrumentDevice.getSecondMenu();
//        processer.put("instrument", "SecondMenu", String.valueOf(secondMenu));
//
//        int airHeatingOilWarn = autoInstrumentDevice.getAirHeatingOilWarn();
//        processer.put("instrument", "AirHeatingOilWarn", String.valueOf(airHeatingOilWarn));
//
//        int chargeAppTimeOption = autoInstrumentDevice.getChargeAppTimeOption();
//        processer.put("instrument", "ChargeAppTimeOption", String.valueOf(chargeAppTimeOption));
//
//        int accDistance2in1 = autoInstrumentDevice.get2in1AccDistance();
//        processer.put("instrument", "2in1AccDistance", String.valueOf(accDistance2in1));
//
//        int accTextPrompt2in1 = autoInstrumentDevice.get2in1AccTextPrompt();
//        processer.put("instrument", "2in1AccTextPrompt", String.valueOf(accTextPrompt2in1));
//
//        int bodyPosition2in1 = autoInstrumentDevice.get2in1BodyPosition();
//        processer.put("instrument", "2in1BodyPosition", String.valueOf(bodyPosition2in1));
//
//        int lineValue = autoInstrumentDevice.getLineValue(0); // Assuming 0 for flag
//        processer.put("instrument", "LineValue", String.valueOf(lineValue));
//
//        int accWorkInterface2in1 = autoInstrumentDevice.get2in1AccWorkInterface();
//        processer.put("instrument", "2in1AccWorkInterface", String.valueOf(accWorkInterface2in1));
//
//        int accTimeDistance2in1 = autoInstrumentDevice.get2in1AccTimeDistance();
//        processer.put("instrument", "2in1AccTimeDistance", String.valueOf(accTimeDistance2in1));
//
//        int soundFreq = autoInstrumentDevice.getSoundFreq();
//        processer.put("instrument", "SoundFreq", String.valueOf(soundFreq));
//
//        int faultSmallLightIndicator2in1 = autoInstrumentDevice.get2in1FaultSmallLightIndicator();
//        processer.put("instrument", "2in1FaultSmallLightIndicator", String.valueOf(faultSmallLightIndicator2in1));
//
//        int faultSmallLightIndicatorColor2in1 = autoInstrumentDevice.get2in1FaultSmallLightIndicatorColor();
//        processer.put("instrument", "2in1FaultSmallLightIndicatorColor", String.valueOf(faultSmallLightIndicatorColor2in1));
//
//        int faultFrontFogLightIndicator2in1 = autoInstrumentDevice.get2in1FaultFrontFogLightIndicator();
//        processer.put("instrument", "2in1FaultFrontFogLightIndicator", String.valueOf(faultFrontFogLightIndicator2in1));
//
//        int faultFrontFogLightIndicatorColor2in1 = autoInstrumentDevice.get2in1FaultFrontFogLightIndicatorColor();
//        processer.put("instrument", "2in1FaultFrontFogLightIndicatorColor", String.valueOf(faultFrontFogLightIndicatorColor2in1));
//
//        int faultGrassIndicator2in1 = autoInstrumentDevice.get2in1FaultGrassIndicator();
//        processer.put("instrument", "2in1FaultGrassIndicator", String.valueOf(faultGrassIndicator2in1));
//
//        int faultGrassIndicatorColor2in1 = autoInstrumentDevice.get2in1FaultGrassIndicatorColor();
//        processer.put("instrument", "2in1FaultGrassIndicatorColor", String.valueOf(faultGrassIndicatorColor2in1));
//
//        int menuState2in1 = autoInstrumentDevice.get2in1MenuState();
//        processer.put("instrument", "2in1MenuState", String.valueOf(menuState2in1));
//
//        int appointmentHour = autoInstrumentDevice.getAppointmentHour();
//        processer.put("instrument", "AppointmentHour", String.valueOf(appointmentHour));
//
//        int appointmentMinute = autoInstrumentDevice.getAppointmentMinute();
//        processer.put("instrument", "AppointmentMinute", String.valueOf(appointmentMinute));
//
//        int instrumentView = autoInstrumentDevice.getInstrumentView();
//        processer.put("instrument", "InstrumentView", String.valueOf(instrumentView));
//
//        int faultMuddyIndicator = autoInstrumentDevice.getFaultMuddyIndicator();
//        processer.put("instrument", "FaultMuddyIndicator", String.valueOf(faultMuddyIndicator));
//
//        int faultMuddyIndicatorColor = autoInstrumentDevice.getFaultMuddyIndicatorColor();
//        processer.put("instrument", "FaultMuddyIndicatorColor", String.valueOf(faultMuddyIndicatorColor));
//
//        int faultNormalIndicator = autoInstrumentDevice.getFaultNormalIndicator();
//        processer.put("instrument", "FaultNormalIndicator", String.valueOf(faultNormalIndicator));
//
//        int faultNormalIndicatorColor = autoInstrumentDevice.getFaultNormalIndicatorColor();
//        processer.put("instrument", "FaultNormalIndicatorColor", String.valueOf(faultNormalIndicatorColor));
//
//        int faultOilLifeDetectIndicator = autoInstrumentDevice.getFaultOilLifeDetectIndicator();
//        processer.put("instrument", "FaultOilLifeDetectIndicator", String.valueOf(faultOilLifeDetectIndicator));
//
//        int faultOilLifeDetectIndicatorColor = autoInstrumentDevice.getFaultOilLifeDetectIndicatorColor();
//        processer.put("instrument", "FaultOilLifeDetectIndicatorColor", String.valueOf(faultOilLifeDetectIndicatorColor));
//
//        int faultSandIndicator = autoInstrumentDevice.getFaultSandIndicator();
//        processer.put("instrument", "FaultSandIndicator", String.valueOf(faultSandIndicator));
//
//        int faultSandIndicatorColor = autoInstrumentDevice.getFaultSandIndicatorColor();
//        processer.put("instrument", "FaultSandIndicatorColor", String.valueOf(faultSandIndicatorColor));
//
//        int kmEneryConsumptionDisplayState50 = autoInstrumentDevice.get50KmEneryConsumptionDisplayState();
//        processer.put("instrument", "50KmEneryConsumptionDisplayState", String.valueOf(kmEneryConsumptionDisplayState50));
//
//        int averageEneryConsumptionDisplayState = autoInstrumentDevice.getAverageEneryConsumptionDisplayState();
//        processer.put("instrument", "AverageEneryConsumptionDisplayState", String.valueOf(averageEneryConsumptionDisplayState));
//
//        int averageFuelConsumptionDisplayState = autoInstrumentDevice.getAverageFuelConsumptionDisplayState();
//        processer.put("instrument", "AverageFuelConsumptionDisplayState", String.valueOf(averageFuelConsumptionDisplayState));
//
//        int fuelConsumptionDisplayState = autoInstrumentDevice.getFuelConsumptionDisplayState();
//        processer.put("instrument", "FuelConsumptionDisplayState", String.valueOf(fuelConsumptionDisplayState));
//
//        int instantFuelConsumptionDisplayState = autoInstrumentDevice.getInstantFuelConsumptionDisplayState();
//        processer.put("instrument", "InstantFuelConsumptionDisplayState", String.valueOf(instantFuelConsumptionDisplayState));
//
//        int instantFuelConsumptionUnit = autoInstrumentDevice.getInstantFuelConsumptionUnit();
//        processer.put("instrument", "InstantFuelConsumptionUnit", String.valueOf(instantFuelConsumptionUnit));
//
//        double waterTempMeterPercent = autoInstrumentDevice.getWaterTempMeterPercent();
//        processer.put("instrument", "WaterTempMeterPercent", String.valueOf(waterTempMeterPercent));
//
//        int airHeatingDisplayState = autoInstrumentDevice.getAirHeatingDisplayState();
//        processer.put("instrument", "AirHeatingDisplayState", String.valueOf(airHeatingDisplayState));
//
//        int directTypePressDisplayState = autoInstrumentDevice.getDirectTypePressDisplayState();
//        processer.put("instrument", "DirectTypePressDisplayState", String.valueOf(directTypePressDisplayState));
//
//        int kmFuelConsumptionDisplayState50 = autoInstrumentDevice.get50KmFuelConsumptionDisplayState();
//        processer.put("instrument", "50KmFuelConsumptionDisplayState", String.valueOf(kmFuelConsumptionDisplayState50));
//
//        int energyDisplay = autoInstrumentDevice.getEnergyDisplay();
//        processer.put("instrument", "EnergyDisplay", String.valueOf(energyDisplay));
//
//        int faultIndicator = autoInstrumentDevice.getFaultIndicator(0); // Assuming 0 for indicatorType
//        processer.put("instrument", "FaultIndicator", String.valueOf(faultIndicator));
//
//        int faultIndicatorColor = autoInstrumentDevice.getFaultIndicatorColor(0); // Assuming 0 for indicatorType
//        processer.put("instrument", "FaultIndicatorColor", String.valueOf(faultIndicatorColor));
//
//        int fuelLowAlarm = autoInstrumentDevice.getFuelLowAlarm();
//        processer.put("instrument", "FuelLowAlarm", String.valueOf(fuelLowAlarm));

        // Panorama Listener
        int panoWorkState = autoPanoramaDevice.getPanoWorkState();
        processer.put("panorama", "PanoWorkState", String.valueOf(panoWorkState));

        int panOutputState = autoPanoramaDevice.getPanoOutputState();
        processer.put("panorama", "PanOutputState", String.valueOf(panOutputState));

        int backLineConfig = autoPanoramaDevice.getBackLineConfig();
        processer.put("panorama", "BackLineConfig", String.valueOf(backLineConfig));

        int panoramaOnlineState = autoPanoramaDevice.getPanoramaOnlineState();
        processer.put("panorama", "PanoramaOnlineState", String.valueOf(panoramaOnlineState));

        int panoRotation = autoPanoramaDevice.getPanoRotation();
        processer.put("panorama", "PanoRotation", String.valueOf(panoRotation));

        int displayMode = autoPanoramaDevice.getDisplayMode();
        processer.put("panorama", "DisplayMode", String.valueOf(displayMode));

        // Radar Listener
        int radarProbeState = autoRadarDevice.getRadarProbeState(0); // Assuming 0 for area
        processer.put("radar", "RadarProbeState", String.valueOf(radarProbeState));

        int reverseRadarSwitchState = autoRadarDevice.getReverseRadarSwitchState();
        processer.put("radar", "ReverseRadarSwitchState", String.valueOf(reverseRadarSwitchState));

        // Sensor Listener
        double temperatureSensorValue = autoSensorDevice.getTemperatureSensorValue();
        processer.put("sensor", "TemperatureSensorValue", String.valueOf(temperatureSensorValue));

        double humiditySensorValue = autoSensorDevice.getHumiditySensorValue();
        processer.put("sensor", "HumiditySensorValue", String.valueOf(humiditySensorValue));

        int lightIntensity = autoSensorDevice.getLightIntensity();
        processer.put("sensor", "LightIntensity", String.valueOf(lightIntensity));

//        int slopeValue = autoSensorDevice.getSlopeValue();
//        processer.put("sensor", "SlopeValue", String.valueOf(slopeValue));

//        byte[] accSensorData = autoSensorDevice.getAccSensorData();
//        processer.put("sensor", "AccSensorData", Arrays.toString(accSensorData));

        // Setting Listener
        int acbtWindSwitch = autoSettingDevice.getACBTWind();
        processer.put("setting", "ACBTWindSwitch", String.valueOf(acbtWindSwitch));

        int acTunnelCycleSwitch = autoSettingDevice.getACTunnelCycle();
        processer.put("setting", "ACTunnelCycleSwitch", String.valueOf(acTunnelCycleSwitch));

        int acPauseCycleSwitch = autoSettingDevice.getACPauseCycle();
        processer.put("setting", "ACPauseCycleSwitch", String.valueOf(acPauseCycleSwitch));

        int acAutoAirMode = autoSettingDevice.getACAutoAir();
        processer.put("setting", "ACAutoAirMode", String.valueOf(acAutoAirMode));

        int energyFeedbackStrength = autoSettingDevice.getEnergyFeedback();
        processer.put("setting", "EnergyFeedbackStrength", String.valueOf(energyFeedbackStrength));

        int socTargetRange = autoSettingDevice.getSOCTarget();
        processer.put("setting", "SOCTargetRange", String.valueOf(socTargetRange));

        int chargingPortSwitch = autoSettingDevice.getChargingPort();
        processer.put("setting", "ChargingPortSwitch", String.valueOf(chargingPortSwitch));

        int steerAssisMode = autoSettingDevice.getSteerAssis();
        processer.put("setting", "SteerAssisMode", String.valueOf(steerAssisMode));

        int rearViewMirrorFlipSwitch = autoSettingDevice.getRearViewMirrorFlip();
        processer.put("setting", "RearViewMirrorFlipSwitch", String.valueOf(rearViewMirrorFlipSwitch));

        int driverSeatAutoReturnSwitch = autoSettingDevice.getDriverSeatAutoReturn();
        processer.put("setting", "DriverSeatAutoReturnSwitch", String.valueOf(driverSeatAutoReturnSwitch));

        int steerPositionAutoReturnSwitch = autoSettingDevice.getSteerPositionAutoReturn();
        processer.put("setting", "SteerPositionAutoReturnSwitch", String.valueOf(steerPositionAutoReturnSwitch));

        int pm25PowerSwitch = autoSettingDevice.getPM25Power();
        processer.put("setting", "PM25PowerSwitch", String.valueOf(pm25PowerSwitch));

        int pm25SwitchCheck = autoSettingDevice.getPM25SwitchCheck();
        processer.put("setting", "PM25SwitchCheck", String.valueOf(pm25SwitchCheck));

        int pm25TimeCheck = autoSettingDevice.getPM25TimeCheck();
        processer.put("setting", "PM25TimeCheck", String.valueOf(pm25TimeCheck));

//        int controlWindowSwitch = autoSettingDevice.getControlWindow();
//        processer.put("setting", "ControlWindowSwitch", String.valueOf(controlWindowSwitch));

        int lockCarRiseWindow = autoSettingDevice.getLockCarRiseWindow();
        processer.put("setting", "LockCarRiseWindow", String.valueOf(lockCarRiseWindow));

        int backHomeLightDelayValue = autoSettingDevice.getBackHomeLightDelayValue();
        processer.put("setting", "BackHomeLightDelayValue", String.valueOf(backHomeLightDelayValue));

        int leftHomeLightDelayValue = autoSettingDevice.getLeftHomeLightDelayValue();
        processer.put("setting", "LeftHomeLightDelayValue", String.valueOf(leftHomeLightDelayValue));

        int lockOffDoor = autoSettingDevice.getLockOff();
        processer.put("setting", "LockOffDoor", String.valueOf(lockOffDoor));

        int remoteControlUpwindowState = autoSettingDevice.getRemoteControlUpwindowState();
        processer.put("setting", "RemoteControlUpwindowState", String.valueOf(remoteControlUpwindowState));

        int microSwitchLockWindowState = autoSettingDevice.getMicroSwitchLockWindowState();
        processer.put("setting", "MicroSwitchLockWindowState", String.valueOf(microSwitchLockWindowState));

        int rearAcOnlineState = autoSettingDevice.getRearAcOnlineState();
        processer.put("setting", "RearAcOnlineState", String.valueOf(rearAcOnlineState));

        int backDoorElectricMode = autoSettingDevice.getBackDoorElectricMode();
        processer.put("setting", "BackDoorElectricMode", String.valueOf(backDoorElectricMode));

        int overspeedLockState = autoSettingDevice.getOverspeedLock();
        processer.put("setting", "OverspeedLockState", String.valueOf(overspeedLockState));

        int language = autoSettingDevice.getLanguage();
        processer.put("setting", "Language", String.valueOf(language));

        int safeWarnState = autoSettingDevice.getSafeWarnState();
        processer.put("setting", "SafeWarnState", String.valueOf(safeWarnState));

        int maintainRemindState = autoSettingDevice.getMaintainRemindState();
        processer.put("setting", "MaintainRemindState", String.valueOf(maintainRemindState));

        int microSwitchUnlockWindowState = autoSettingDevice.getMicroSwitchUnlockWindowState();
        processer.put("setting", "MicroSwitchUnlockWindowState", String.valueOf(microSwitchUnlockWindowState));

        int autoExternalRearMirrorFollowUpSwitch = autoSettingDevice.getAutoExternalRearMirrorFollowUpSwitch();
        processer.put("setting", "AutoExternalRearMirrorFollowUpSwitch", String.valueOf(autoExternalRearMirrorFollowUpSwitch));

        // Speed Listener
        double speed = autoSpeedDevice.getCurrentSpeed();
        processer.put("speed", "Speed", String.valueOf(speed));

        int accelerateDeepness = autoSpeedDevice.getAccelerateDeepness();
        processer.put("speed", "AccelerateDeepness", String.valueOf(accelerateDeepness));

        int brakeDeepness = autoSpeedDevice.getBrakeDeepness();
        processer.put("speed", "BrakeDeepness", String.valueOf(brakeDeepness));

        // Statistic Listener
        int totalMileageValue = autoStatisticDevice.getTotalMileageValue();
        processer.put("statistic", "TotalMileageValue", String.valueOf(totalMileageValue));

        double totalFuelCon = autoStatisticDevice.getTotalFuelConValue();
        processer.put("statistic", "TotalFuelCon", String.valueOf(totalFuelCon));

        double totalElecCon = autoStatisticDevice.getTotalElecConValue();
        processer.put("statistic", "TotalElecCon", String.valueOf(totalElecCon));

        double drivingTime = autoStatisticDevice.getDrivingTimeValue();
        processer.put("statistic", "DrivingTime", String.valueOf(drivingTime));

        double lastFuelConPHM = autoStatisticDevice.getLastFuelConPHMValue();
        processer.put("statistic", "LastFuelConPHM", String.valueOf(lastFuelConPHM));

        double totalFuelConPHM = autoStatisticDevice.getTotalFuelConPHMValue();
        processer.put("statistic", "TotalFuelConPHM", String.valueOf(totalFuelConPHM));

        double lastElecConPHM = autoStatisticDevice.getLastElecConPHMValue();
        processer.put("statistic", "LastElecConPHM", String.valueOf(lastElecConPHM));

        double totalElecConPHM = autoStatisticDevice.getTotalElecConPHMValue();
        processer.put("statistic", "TotalElecConPHM", String.valueOf(totalElecConPHM));

        int elecDrivingRange = autoStatisticDevice.getElecDrivingRangeValue();
        processer.put("statistic", "ElecDrivingRange", String.valueOf(elecDrivingRange));

        int fuelDrivingRange = autoStatisticDevice.getFuelDrivingRangeValue();
        processer.put("statistic", "FuelDrivingRange", String.valueOf(fuelDrivingRange));

        int fuelPercentage = autoStatisticDevice.getFuelPercentageValue();
        processer.put("statistic", "FuelPercentage", String.valueOf(fuelPercentage));

        double elecPercentage = autoStatisticDevice.getElecPercentageValue();
        processer.put("statistic", "ElecPercentage", String.valueOf(elecPercentage));

        int keyBatteryLevel = autoStatisticDevice.getKeyBatteryLevel();
        processer.put("statistic", "KeyBatteryLevel", String.valueOf(keyBatteryLevel));

        int evMileageValue = autoStatisticDevice.getEVMileageValue();
        processer.put("statistic", "EVMileageValue", String.valueOf(evMileageValue));

        int hevMileageValue = autoStatisticDevice.getHEVMileageValue();
        processer.put("statistic", "HEVMileageValue", String.valueOf(hevMileageValue));

        int waterTemperature = autoStatisticDevice.getWaterTemperature();
        processer.put("statistic", "WaterTemperature", String.valueOf(waterTemperature));

        double instantElecCon = autoStatisticDevice.getInstantElecConValue();
        processer.put("statistic", "InstantElecCon", String.valueOf(instantElecCon));

        double instantFuelCon = autoStatisticDevice.getInstantFuelConValue();
        processer.put("statistic", "InstantFuelCon", String.valueOf(instantFuelCon));

        // Tyre Listener
        int tyreSystemState = autoTyreDevice.getTyreSystemState();
        processer.put("tyre", "TyreSystemState", String.valueOf(tyreSystemState));

        int tyreTemperatureState = autoTyreDevice.getTyreTemperatureState();
        processer.put("tyre", "TyreTemperatureState", String.valueOf(tyreTemperatureState));

        int tyreBatteryState = autoTyreDevice.getTyreBatteryState();
        processer.put("tyre", "TyreBatteryState", String.valueOf(tyreBatteryState));

        int tyreAirLeakState = autoTyreDevice.getTyreAirLeakState(0); // Assuming 0 for area
        processer.put("tyre", "TyreAirLeakState", String.valueOf(tyreAirLeakState));

        int tyreSignalState = autoTyreDevice.getTyreSignalState(0); // Assuming 0 for area
        processer.put("tyre", "TyreSignalState", String.valueOf(tyreSignalState));

        int tyrePressureState = autoTyreDevice.getTyrePressureState(0); // Assuming 0 for area
        processer.put("tyre", "TyrePressureState", String.valueOf(tyrePressureState));

        int tyrePressureValue = autoTyreDevice.getTyrePressureValue(0); // Assuming 0 for area
        processer.put("tyre", "TyrePressureValue", String.valueOf(tyrePressureValue));
    }

    AbsBYDAutoAcListener acListener = new AbsBYDAutoAcListener() {
        @Override
        public void onDataEventChanged(int eventType, BYDAutoEventValue eventValue) {
            super.onDataEventChanged(eventType, eventValue);
            processer.put("ac", "DataEvent", String.valueOf(eventType) + ", " + String.valueOf(eventValue));
        }

        @Override
        public void onError(int errCode, String errMessage) {
            super.onError(errCode, errMessage);
            processer.put("ac", "Error", String.valueOf(errCode) + ", " + errMessage);
        }

        @Override
        public void onAcWindModeShownStateChanged(int state) {
            super.onAcWindModeShownStateChanged(state);
            processer.put("ac", "AcWindModeShownState", String.valueOf(state));
        }

        @Override
        public void onTemperatureChanged(int area, int value) {
            super.onTemperatureChanged(area, value);
            processer.put("ac", "Temperature", String.valueOf(area) + ", " + String.valueOf(value));
        }

        @Override
        public void onTemperatureUnitChanged(int unit) {
            super.onTemperatureUnitChanged(unit);
            processer.put("ac", "TemperatureUnit", String.valueOf(unit));
        }

        @Override
        public void onAcWindLevelChanged(int level) {
            super.onAcWindLevelChanged(level);
            processer.put("ac", "AcWindLevel", String.valueOf(level));
        }

        @Override
        public void onAcWindLevelManualSignChanged(int sign) {
            super.onAcWindLevelManualSignChanged(sign);
            processer.put("ac", "AcWindLevelManualSign", String.valueOf(sign));
        }

        @Override
        public void onAcWindModeChanged(int mode) {
            super.onAcWindModeChanged(mode);
            processer.put("ac", "AcWindMode", String.valueOf(mode));
        }

        @Override
        public void onAcWindModeManualSignChanged(int sign) {
            super.onAcWindModeManualSignChanged(sign);
            processer.put("ac", "AcWindModeManualSign", String.valueOf(sign));
        }

        @Override
        public void onAcCompressorModeChanged(int mode) {
            super.onAcCompressorModeChanged(mode);
            processer.put("ac", "AcCompressorMode", String.valueOf(mode));
        }

        @Override
        public void onAcCompressorManualSignChanged(int sign) {
            super.onAcCompressorManualSignChanged(sign);
            processer.put("ac", "AcCompressorManualSign", String.valueOf(sign));
        }

        @Override
        public void onAcDefrostStateChanged(int area, int state) {
            super.onAcDefrostStateChanged(area, state);
            processer.put("ac", "AcDefrostState", String.valueOf(area) + ", " + String.valueOf(state));
        }

        @Override
        public void onAcVentilationStateChanged(int state) {
            super.onAcVentilationStateChanged(state);
            processer.put("ac", "AcVentilationState", String.valueOf(state));
        }

        @Override
        public void onAcCycleModeChanged(int mode) {
            super.onAcCycleModeChanged(mode);
            processer.put("ac", "AcCycleMode", String.valueOf(mode));
        }

        @Override
        public void onAcCtrlModeChanged(int mode) {
            super.onAcCtrlModeChanged(mode);
            processer.put("ac", "AcCtrlMode", String.valueOf(mode));
        }

        @Override
        public void onAcRearStoped() {
            super.onAcRearStoped();
            processer.put("ac", "AcRearStoped", "");
        }

        @Override
        public void onAcRearStarted() {
            super.onAcRearStarted();
            processer.put("ac", "AcRearStarted", "");
        }

        @Override
        public void onAcStoped() {
            super.onAcStoped();
            processer.put("ac", "AcStoped", "");
        }

        @Override
        public void onAcStarted() {
            super.onAcStarted();
            processer.put("ac", "AcStarted", "");
        }
    };

    AbsBYDAutoBodyworkListener bodyworkListener = new AbsBYDAutoBodyworkListener() {
        @Override
        public void onWindowStateChanged(int area, int state) {
            super.onWindowStateChanged(area, state);
            processer.put("bodywork", "WindowState", String.valueOf(area) + ", " + String.valueOf(state));
        }

        @Override
        public void onDoorStateChanged(int area, int state) {
            super.onDoorStateChanged(area, state);
            processer.put("bodywork", "DoorState", String.valueOf(area) + ", " + String.valueOf(state));
        }

        @Override
        public void onAutoSystemStateChanged(int state) {
            super.onAutoSystemStateChanged(state);
            processer.put("bodywork", "AutoSystemState", String.valueOf(state));
        }

        @Override
        public void onSteeringWheelValueChanged(int type, double value) {
            super.onSteeringWheelValueChanged(type, value);
            processer.put("bodywork", "SteeringWheelValue", String.valueOf(type) + ", " + String.valueOf(value));
        }

        @Override
        public void onSteeringWheelAngleChanged(double value, int sensorState, int calibrationState) {
            super.onSteeringWheelAngleChanged(value, sensorState, calibrationState);
            processer.put("bodywork", "SteeringWheelAngle", String.valueOf(value) + ", " + String.valueOf(sensorState) + ", " + String.valueOf(calibrationState));
        }

        @Override
        public void onSteeringWheelSpeedChanged(double value, int sensorState, int calibrationState) {
            super.onSteeringWheelSpeedChanged(value, sensorState, calibrationState);
            processer.put("bodywork", "SteeringWheelSpeed", String.valueOf(value) + ", " + String.valueOf(sensorState) + ", " + String.valueOf(calibrationState));
        }

        @Override
        public void onPowerLevelChanged(int level) {
            super.onPowerLevelChanged(level);
            processer.put("bodywork", "PowerLevel", String.valueOf(level));
        }

        @Override
        public void onBatteryVoltageLevelChanged(int level) {
            super.onBatteryVoltageLevelChanged(level);
            processer.put("bodywork", "BatteryVoltageLevel", String.valueOf(level));
        }

        @Override
        public void onPowerDayModeChanged(int state) {
            super.onPowerDayModeChanged(state);
            processer.put("bodywork", "PowerDayMode", String.valueOf(state));
        }

        @Override
        public void onAutoVINChanged(String vin) {
            super.onAutoVINChanged(vin);
            processer.put("bodywork", "AutoVIN", vin);
        }

        @Override
        public void onMoonRoofConfigChanged(int config) {
            super.onMoonRoofConfigChanged(config);
            processer.put("bodywork", "MoonRoofConfig", String.valueOf(config));
        }

        @Override
        public void onFuelElecLowPowerChanged(int state) {
            super.onFuelElecLowPowerChanged(state);
            processer.put("bodywork", "FuelElecLowPower", String.valueOf(state));
        }

        @Override
        public void onAlarmStateChanged(int state) {
            super.onAlarmStateChanged(state);
            processer.put("bodywork", "AlarmState", String.valueOf(state));
        }

        @Override
        public void onWindowOpenPercentChanged(int area, int percent) {
            super.onWindowOpenPercentChanged(area, percent);
            processer.put("bodywork", "WindowOpenPercent", String.valueOf(area) + ", " + String.valueOf(percent));
        }

        @Override
        public void onCarWindowAntiPinchConfigChanged(int config) {
            super.onCarWindowAntiPinchConfigChanged(config);
            processer.put("bodywork", "CarWindowAntiPinchConfig", String.valueOf(config));
        }

        @Override
        public void onRainCloseWindowChanged(int state) {
            super.onRainCloseWindowChanged(state);
            processer.put("bodywork", "RainCloseWindow", String.valueOf(state));
        }

        @Override
        public void onMessage5sOnlineStateChanged(int id, int state) {
            super.onMessage5sOnlineStateChanged(id, state);
            processer.put("bodywork", "Message5sOnlineState", String.valueOf(id) + ", " + String.valueOf(state));
        }

        @Override
        public void onHasMessageChanged(int id, int state) {
            super.onHasMessageChanged(id, state);
            processer.put("bodywork", "HasMessage", String.valueOf(id) + ", " + String.valueOf(state));
        }

        @Override
        public void onSunroofStateChanged(int state) {
            super.onSunroofStateChanged(state);
            processer.put("bodywork", "SunroofState", String.valueOf(state));
        }

        @Override
        public void onWindowPermitStateChanged(int state) {
            super.onWindowPermitStateChanged(state);
            processer.put("bodywork", "WindowPermitState", String.valueOf(state));
        }

        @Override
        public void onWindoblindInitStateChanged(int state) {
            super.onWindoblindInitStateChanged(state);
            processer.put("bodywork", "WindoblindInitState", String.valueOf(state));
        }

        @Override
        public void onSunroofInitStateChanged(int state) {
            super.onSunroofInitStateChanged(state);
            processer.put("bodywork", "SunroofInitState", String.valueOf(state));
        }

        @Override
        public void onSunroofCloseNoticeChanged(int state) {
            super.onSunroofCloseNoticeChanged(state);
            processer.put("bodywork", "SunroofCloseNotice", String.valueOf(state));
        }

        @Override
        public void onSunroofWindowblindPositionChanged(int value) {
            super.onSunroofWindowblindPositionChanged(value);
            processer.put("bodywork", "SunroofWindowblindPosition", String.valueOf(value));
        }

        @Override
        public void onSunroofPositionChanged(int value) {
            super.onSunroofPositionChanged(value);
            processer.put("bodywork", "SunroofPosition", String.valueOf(value));
        }

        @Override
        public void onSmartVoiceLimitChanged(int value) {
            super.onSmartVoiceLimitChanged(value);
            processer.put("bodywork", "SmartVoiceLimit", String.valueOf(value));
        }

        @Override
        public void onBatteryPowerChanged(int value) {
            super.onBatteryPowerChanged(value);
            processer.put("bodywork", "BatteryPower", String.valueOf(value));
        }

        @Override
        public void onError(int errCode, String errMessage) {
            super.onError(errCode, errMessage);
            processer.put("bodywork", "Error", String.valueOf(errCode) + ", " + errMessage);
        }

        @Override
        public void onDataEventChanged(int eventType, BYDAutoEventValue eventValue) {
            super.onDataEventChanged(eventType, eventValue);
            processer.put("bodywork", "DataEvent", String.valueOf(eventType) + ", " + String.valueOf(eventValue));
        }
    };

    AbsBYDAutoChargingListener chargingListener = new AbsBYDAutoChargingListener() {
        @Override
        public void onChargerFaultStateChanged(int state) {
            super.onChargerFaultStateChanged(state);
            processer.put("charging", "ChargerFaultState", String.valueOf(state));
        }

        @Override
        public void onChargerWorkStateChanged(int state) {
            super.onChargerWorkStateChanged(state);
            processer.put("charging", "ChargerWorkState", String.valueOf(state));
        }

        @Override
        public void onChargingCapacityChanged(double value) {
            super.onChargingCapacityChanged(value);
            processer.put("charging", "ChargingCapacity", String.valueOf(value));
        }

        @Override
        public void onChargingTypeChanged(int type) {
            super.onChargingTypeChanged(type);
            processer.put("charging", "ChargingType", String.valueOf(type));
        }

        @Override
        public void onChargingRestTimeChanged(int hour, int min) {
            super.onChargingRestTimeChanged(hour, min);
            processer.put("charging", "ChargingRestTime", String.valueOf(hour) + ", " + String.valueOf(min));
        }

        @Override
        public void onChargingCapStateChanged(int type, int state) {
            super.onChargingCapStateChanged(type, state);
            processer.put("charging", "ChargingCapState", String.valueOf(type) + ", " + String.valueOf(state));
        }

        @Override
        public void onChargingPortLockRebackStateChanged(int state) {
            super.onChargingPortLockRebackStateChanged(state);
            processer.put("charging", "ChargingPortLockRebackState", String.valueOf(state));
        }

        @Override
        public void onDischargeRequestStateChanged(int state) {
            super.onDischargeRequestStateChanged(state);
            processer.put("charging", "DischargeRequestState", String.valueOf(state));
        }

        @Override
        public void onChargerStateChanged(int state) {
            super.onChargerStateChanged(state);
            processer.put("charging", "ChargerState", String.valueOf(state));
        }

        @Override
        public void onChargingGunStateChanged(int state) {
            super.onChargingGunStateChanged(state);
            processer.put("charging", "ChargingGunState", String.valueOf(state));
        }

        @Override
        public void onChargingPowerChanged(double value) {
            super.onChargingPowerChanged(value);
            processer.put("charging", "ChargingPower", String.valueOf(value));
        }

        @Override
        public void onBatteryManagementDeviceStateChanged(int state) {
            super.onBatteryManagementDeviceStateChanged(state);
            processer.put("charging", "BatteryManagementDeviceState", String.valueOf(state));
        }

        @Override
        public void onChargingScheduleEnableStateChanged(int state) {
            super.onChargingScheduleEnableStateChanged(state);
            processer.put("charging", "ChargingScheduleEnableState", String.valueOf(state));
        }

        @Override
        public void onChargingScheduleStateChanged(int state) {
            super.onChargingScheduleStateChanged(state);
            processer.put("charging", "ChargingScheduleState", String.valueOf(state));
        }

        @Override
        public void onChargingGunNotInsertedStateChanged(int state) {
            super.onChargingGunNotInsertedStateChanged(state);
            processer.put("charging", "ChargingGunNotInsertedState", String.valueOf(state));
        }

        @Override
        public void onChargingScheduleTimeChanged(int hour, int min) {
            super.onChargingScheduleTimeChanged(hour, min);
            processer.put("charging", "ChargingScheduleTime", String.valueOf(hour) + ", " + String.valueOf(min));
        }

        @Override
        public void onError(int errCode, String errMessage) {
            super.onError(errCode, errMessage);
            processer.put("charging", "Error", String.valueOf(errCode) + ", " + errMessage);
        }

        @Override
        public void onDataEventChanged(int eventType, BYDAutoEventValue eventValue) {
            super.onDataEventChanged(eventType, eventValue);
            processer.put("charging", "DataEvent", String.valueOf(eventType) + ", " + String.valueOf(eventValue));
        }

        @Override
        public void onChargingStateChanged(int state) {
            super.onChargingStateChanged(state);
            processer.put("charging", "ChargingState", String.valueOf(state));
        }

        @Override
        public void onChargingModeChanged(int mode) {
            super.onChargingModeChanged(mode);
            processer.put("charging", "ChargingMode", String.valueOf(mode));
        }

        @Override
        public void onChargingTimerInfoChanged(ChargingTimerInfo timerInfo) {
            super.onChargingTimerInfoChanged(timerInfo);
            processer.put("charging", "ChargingTimerInfo", String.valueOf(timerInfo));
        }

        @Override
        public void onWirelessChargingSwitchStateChanged(int state) {
            super.onWirelessChargingSwitchStateChanged(state);
            processer.put("charging", "WirelessChargingSwitchState", String.valueOf(state));
        }

        @Override
        public void onWirelessChargingOnline5sStateChanged(int state) {
            super.onWirelessChargingOnline5sStateChanged(state);
            processer.put("charging", "WirelessChargingOnline5sState", String.valueOf(state));
        }

        @Override
        public void onSmartChargingStateChanged(int state) {
            super.onSmartChargingStateChanged(state);
            processer.put("charging", "SmartChargingState", String.valueOf(state));
        }

        @Override
        public void onDischargeStateChanged(int type, int state) {
            super.onDischargeStateChanged(type, state);
            processer.put("charging", "DischargeState", String.valueOf(type) + ", " + String.valueOf(state));
        }

        @Override
        public void onDisChargeWarningStateChanged(int state) {
            super.onDisChargeWarningStateChanged(state);
            processer.put("charging", "DisChargeWarningState", String.valueOf(state));
        }

        @Override
        public void onFeatureChanged(String feature, int ifHas) {
            super.onFeatureChanged(feature, ifHas);
            processer.put("charging", "Feature", feature + ", " + String.valueOf(ifHas));
        }

        @Override
        public void onWirlessChargingStateChanged(int state) {
            super.onWirlessChargingStateChanged(state);
            processer.put("charging", "WirlessChargingState", String.valueOf(state));
        }

        @Override
        public void onChargeTempCtlStateChanged(int state) {
            super.onChargeTempCtlStateChanged(state);
            processer.put("charging", "ChargeTempCtlState", String.valueOf(state));
        }

        @Override
        public void onBatteryTypeChanged(int type) {
            super.onBatteryTypeChanged(type);
            processer.put("charging", "BatteryType", String.valueOf(type));
        }

        @Override
        public void onChargeStopSwitchStateChanged(int state) {
            super.onChargeStopSwitchStateChanged(state);
            processer.put("charging", "ChargeStopSwitchState", String.valueOf(state));
        }

        @Override
        public void onChargeStopCapacityStateChanged(int state) {
            super.onChargeStopCapacityStateChanged(state);
            processer.put("charging", "ChargeStopCapacityState", String.valueOf(state));
        }

        @Override
        public void onWeatherAndTimeRequestChanged(int state) {
            super.onWeatherAndTimeRequestChanged(state);
            processer.put("charging", "WeatherAndTimeRequest", String.valueOf(state));
        }

        @Override
        public void onCarDischargeStateChanged(int state) {
            super.onCarDischargeStateChanged(state);
            processer.put("charging", "CarDischargeState", String.valueOf(state));
        }

        @Override
        public void onCarDischargeLowWarnChanged(int state) {
            super.onCarDischargeLowWarnChanged(state);
            processer.put("charging", "CarDischargeLowWarn", String.valueOf(state));
        }

        @Override
        public void onVtovDischargeConnectStateChanged(int value) {
            super.onVtovDischargeConnectStateChanged(value);
            processer.put("charging", "VtovDischargeConnectState", String.valueOf(value));
        }

        @Override
        public void onVtovDischargeLimitValChanged(int value) {
            super.onVtovDischargeLimitValChanged(value);
            processer.put("charging", "VtovDischargeLimitVal", String.valueOf(value));
        }

        @Override
        public void onVtovDischargeLowestValChanged(int value) {
            super.onVtovDischargeLowestValChanged(value);
            processer.put("charging", "VtovDischargeLowestVal", String.valueOf(value));
        }

        @Override
        public void onVtovDischargeQuantityChanged(double value) {
            super.onVtovDischargeQuantityChanged(value);
            processer.put("charging", "VtovDischargeQuantity", String.valueOf(value));
        }

        @Override
        public void onCapStateChanged(int value) {
            super.onCapStateChanged(value);
            processer.put("charging", "CapState", String.valueOf(value));
        }

        @Override
        public void onSocSaveSwitchChanged(int value) {
            super.onSocSaveSwitchChanged(value);
            processer.put("charging", "SocSaveSwitch", String.valueOf(value));
        }
    };

    AbsBYDAutoDoorLockListener doorLockListener = new AbsBYDAutoDoorLockListener() {
        @Override
        public void onDoorLockStatusChanged(int area, int state) {
            super.onDoorLockStatusChanged(area, state);
            processer.put("doorlock", "DoorLockStatus", String.valueOf(area) + ", " + String.valueOf(state));
        }

        @Override
        public void onError(int errCode, String errMessage) {
            super.onError(errCode, errMessage);
            processer.put("doorlock", "Error", String.valueOf(errCode) + ", " + errMessage);
        }

        @Override
        public void onDataEventChanged(int eventType, BYDAutoEventValue eventValue) {
            super.onDataEventChanged(eventType, eventValue);
            processer.put("doorlock", "DataEvent", String.valueOf(eventType) + ", " + String.valueOf(eventValue));
        }
    };

    AbsBYDAutoEnergyListener energyListener = new AbsBYDAutoEnergyListener() {
        @Override
        public void onEnergyModeChanged(int mode) {
            super.onEnergyModeChanged(mode);
            processer.put("energy", "EnergyMode", String.valueOf(mode));
        }

        @Override
        public void onOperationModeChanged(int mode) {
            super.onOperationModeChanged(mode);
            processer.put("energy", "OperationMode", String.valueOf(mode));
        }

        @Override
        public void onPowerGenerationStateChanged(int mode) {
            super.onPowerGenerationStateChanged(mode);
            processer.put("energy", "PowerGenerationState", String.valueOf(mode));
        }

        @Override
        public void onPowerGenerationValueChanged(int value) {
            super.onPowerGenerationValueChanged(value);
            processer.put("energy", "PowerGenerationValue", String.valueOf(value));
        }

        @Override
        public void onRoadSurfaceChanged(int type) {
            super.onRoadSurfaceChanged(type);
            processer.put("energy", "RoadSurface", String.valueOf(type));
        }

        @Override
        public void onError(int errCode, String errMessage) {
            super.onError(errCode, errMessage);
            processer.put("energy", "Error", String.valueOf(errCode) + ", " + errMessage);
        }

        @Override
        public void onDataEventChanged(int eventType, BYDAutoEventValue eventValue) {
            super.onDataEventChanged(eventType, eventValue);
            processer.put("energy", "DataEvent", String.valueOf(eventType) + ", " + String.valueOf(eventValue));
        }
    };

    AbsBYDAutoEngineListener engineListener = new AbsBYDAutoEngineListener() {
        @Override
        public void onEngineSpeedChanged(int value) {
            super.onEngineSpeedChanged(value);
            processer.put("engine", "EngineSpeed", String.valueOf(value));
        }

        @Override
        public void onEngineCoolantLevelChanged(int state) {
            super.onEngineCoolantLevelChanged(state);
            processer.put("engine", "EngineCoolantLevel", String.valueOf(state));
        }

        @Override
        public void onOilLevelChanged(int value) {
            super.onOilLevelChanged(value);
            processer.put("engine", "OilLevel", String.valueOf(value));
        }

        @Override
        public void onError(int errCode, String errMessage) {
            super.onError(errCode, errMessage);
            processer.put("engine", "Error", String.valueOf(errCode) + ", " + errMessage);
        }

        @Override
        public void onDataEventChanged(int eventType, BYDAutoEventValue eventValue) {
            super.onDataEventChanged(eventType, eventValue);
            processer.put("engine", "DataEvent", String.valueOf(eventType) + ", " + String.valueOf(eventValue));
        }
    };

    AbsBYDAutoGearboxListener gearboxListener = new AbsBYDAutoGearboxListener() {
        @Override
        public void onGearboxAutoModeTypeChanged(int level) {
            super.onGearboxAutoModeTypeChanged(level);
            processer.put("gearbox", "GearboxAutoModeType", String.valueOf(level));
        }

        @Override
        public void onGearboxManualModeLevelChanged(int level) {
            super.onGearboxManualModeLevelChanged(level);
            processer.put("gearbox", "GearboxManualModeLevel", String.valueOf(level));
        }

        @Override
        public void onBrakeFluidLevelChanged(int level) {
            super.onBrakeFluidLevelChanged(level);
            processer.put("gearbox", "BrakeFluidLevel", String.valueOf(level));
        }

        @Override
        public void onParkBrakeSwitchChanged(int level) {
            super.onParkBrakeSwitchChanged(level);
            processer.put("gearbox", "ParkBrakeSwitch", String.valueOf(level));
        }

        @Override
        public void onBrakePedalStateChanged(int level) {
            super.onBrakePedalStateChanged(level);
            processer.put("gearbox", "BrakePedalState", String.valueOf(level));
        }

        @Override
        public void onError(int errCode, String errMessage) {
            super.onError(errCode, errMessage);
            processer.put("gearbox", "Error", String.valueOf(errCode) + ", " + errMessage);
        }

        @Override
        public void onDataEventChanged(int eventType, BYDAutoEventValue eventValue) {
            super.onDataEventChanged(eventType, eventValue);
            processer.put("gearbox", "DataEvent", String.valueOf(eventType) + ", " + String.valueOf(eventValue));
        }
    };

    AbsBYDAutoInstrumentListener instrumentListener = new AbsBYDAutoInstrumentListener() {
        @Override
        public void onMalfunctionInfoChanged(int typeName, int hasMalfunction) {
            super.onMalfunctionInfoChanged(typeName, hasMalfunction);
            processer.put("instrument", "MalfunctionInfo", String.valueOf(typeName) + ", " + String.valueOf(hasMalfunction));
        }

        @Override
        public void onMalfunctionInfoChanged2(int typeName, int hasMalfunction) {
            super.onMalfunctionInfoChanged2(typeName, hasMalfunction);
            processer.put("instrument", "MalfunctionInfo2", String.valueOf(typeName) + ", " + String.valueOf(hasMalfunction));
        }

        @Override
        public void onBacklightModeStateChanged(int backlightMode, int state) {
            super.onBacklightModeStateChanged(backlightMode, state);
            processer.put("instrument", "BacklightModeState", String.valueOf(backlightMode) + ", " + String.valueOf(state));
        }

        @Override
        public void onBacklightBrightnessChanged(int brightness) {
            super.onBacklightBrightnessChanged(brightness);
            processer.put("instrument", "BacklightBrightness", String.valueOf(brightness));
        }

        @Override
        public void onUnitChanged(int unitName, int unitValue) {
            super.onUnitChanged(unitName, unitValue);
            processer.put("instrument", "Unit", String.valueOf(unitName) + ", " + String.valueOf(unitValue));
        }

        @Override
        public void onMaintenanceInfoChanged(int typeName, int infoValue) {
            super.onMaintenanceInfoChanged(typeName, infoValue);
            processer.put("instrument", "MaintenanceInfo", String.valueOf(typeName) + ", " + String.valueOf(infoValue));
        }

        @Override
        public void onMusicInfoResultChanged(int result) {
            super.onMusicInfoResultChanged(result);
            processer.put("instrument", "MusicInfoResult", String.valueOf(result));
        }

        @Override
        public void onCallInfoResultChanged(int result) {
            super.onCallInfoResultChanged(result);
            processer.put("instrument", "CallInfoResult", String.valueOf(result));
        }

        @Override
        public void onRadioInfoResultChanged(int result) {
            super.onRadioInfoResultChanged(result);
            processer.put("instrument", "RadioInfoResult", String.valueOf(result));
        }

        @Override
        public void onAlarmBuzzleStateChange(int state) {
            super.onAlarmBuzzleStateChange(state);
            processer.put("instrument", "AlarmBuzzleState", String.valueOf(state));
        }

        @Override
        public void onPowerOnErrInfoChanged(int err) {
            super.onPowerOnErrInfoChanged(err);
            processer.put("instrument", "PowerOnErrInfo", String.valueOf(err));
        }

        @Override
        public void onPowerOffErrInfoChanged(int err) {
            super.onPowerOffErrInfoChanged(err);
            processer.put("instrument", "PowerOffErrInfo", String.valueOf(err));
        }

        @Override
        public void onRemoteDrivingReminderChanged(int value) {
            super.onRemoteDrivingReminderChanged(value);
            processer.put("instrument", "RemoteDrivingReminder", String.valueOf(value));
        }

        @Override
        public void onKeyDetectionReminderChanged(int value) {
            super.onKeyDetectionReminderChanged(value);
            processer.put("instrument", "KeyDetectionReminder", String.valueOf(value));
        }

        @Override
        public void onAverageSpeedChanged(int value) {
            super.onAverageSpeedChanged(value);
            processer.put("instrument", "AverageSpeed", String.valueOf(value));
        }

        @Override
        public void onExternalChargingPowerChanged(double value) {
            super.onExternalChargingPowerChanged(value);
            processer.put("instrument", "ExternalChargingPower", String.valueOf(value));
        }

        @Override
        public void onInstrumentScreenTypeChanged(int value) {
            super.onInstrumentScreenTypeChanged(value);
            processer.put("instrument", "InstrumentScreenType", String.valueOf(value));
        }

        @Override
        public void onNaviDestinationCommandChanged(int command) {
            super.onNaviDestinationCommandChanged(command);
            processer.put("instrument", "NaviDestinationCommand", String.valueOf(command));
        }

        @Override
        public void onRoadNameCheckStateChanged(int state) {
            super.onRoadNameCheckStateChanged(state);
            processer.put("instrument", "RoadNameCheckState", String.valueOf(state));
        }

        @Override
        public void onTextInfoChanged(int value) {
            super.onTextInfoChanged(value);
            processer.put("instrument", "TextInfo", String.valueOf(value));
        }

        @Override
        public void onModuleStateChanged(int module, int state) {
            super.onModuleStateChanged(module, state);
            processer.put("instrument", "ModuleState", String.valueOf(module) + ", " + String.valueOf(state));
        }

        @Override
        public void onDoorStateChanged(int area, int state) {
            super.onDoorStateChanged(area, state);
            processer.put("instrument", "DoorState", String.valueOf(area) + ", " + String.valueOf(state));
        }

        @Override
        public void onSafetyBeltStatusChanged(int area, int state) {
            super.onSafetyBeltStatusChanged(area, state);
            processer.put("instrument", "SafetyBeltStatus", String.valueOf(area) + ", " + String.valueOf(state));
        }

        @Override
        public void onWheelColorChanged(int position, int state) {
            super.onWheelColorChanged(position, state);
            processer.put("instrument", "WheelColor", String.valueOf(position) + ", " + String.valueOf(state));
        }

        @Override
        public void onWheelTemperatureColorChanged(int position, int state) {
            super.onWheelTemperatureColorChanged(position, state);
            processer.put("instrument", "WheelTemperatureColor", String.valueOf(position) + ", " + String.valueOf(state));
        }

        @Override
        public void onWheelPressureChanged(int position, int value) {
            super.onWheelPressureChanged(position, value);
            processer.put("instrument", "WheelPressure", String.valueOf(position) + ", " + String.valueOf(value));
        }

        @Override
        public void onWheelTemperatureChanged(int position, int value) {
            super.onWheelTemperatureChanged(position, value);
            processer.put("instrument", "WheelTemperature", String.valueOf(position) + ", " + String.valueOf(value));
        }

        @Override
        public void onDeviationStateChanged(int state) {
            super.onDeviationStateChanged(state);
            processer.put("instrument", "DeviationState", String.valueOf(state));
        }

        @Override
        public void onGapDetectionChanged(int state) {
            super.onGapDetectionChanged(state);
            processer.put("instrument", "GapDetection", String.valueOf(state));
        }

        @Override
        public void onLaneLineStateChanged(int state) {
            super.onLaneLineStateChanged(state);
            processer.put("instrument", "LaneLineState", String.valueOf(state));
        }

        @Override
        public void onTimeIntervalStateChanged(int state) {
            super.onTimeIntervalStateChanged(state);
            processer.put("instrument", "TimeIntervalState", String.valueOf(state));
        }

        @Override
        public void onEnergyFeedbackChanged(int state) {
            super.onEnergyFeedbackChanged(state);
            processer.put("instrument", "EnergyFeedback", String.valueOf(state));
        }

        @Override
        public void onTextColorChanged(int state) {
            super.onTextColorChanged(state);
            processer.put("instrument", "TextColor", String.valueOf(state));
        }

        @Override
        public void onSpacingStateChanged(int state) {
            super.onSpacingStateChanged(state);
            processer.put("instrument", "SpacingState", String.valueOf(state));
        }

        @Override
        public void onSoundTypeChanged(int state) {
            super.onSoundTypeChanged(state);
            processer.put("instrument", "SoundType", String.valueOf(state));
        }

        @Override
        public void onAccCruisingSpeedChanged(int state) {
            super.onAccCruisingSpeedChanged(state);
            processer.put("instrument", "AccCruisingSpeed", String.valueOf(state));
        }

        @Override
        public void onAccCruisingSpeedColorChanged(int state) {
            super.onAccCruisingSpeedColorChanged(state);
            processer.put("instrument", "AccCruisingSpeedColor", String.valueOf(state));
        }

        @Override
        public void onPCWAlarmInstructionChanged(int state) {
            super.onPCWAlarmInstructionChanged(state);
            processer.put("instrument", "PCWAlarmInstruction", String.valueOf(state));
        }

        @Override
        public void onLaneLineColorChanged(int state) {
            super.onLaneLineColorChanged(state);
            processer.put("instrument", "LaneLineColor", String.valueOf(state));
        }

        @Override
        public void onTotalMileageChanged(int state) {
            super.onTotalMileageChanged(state);
            processer.put("instrument", "TotalMileage", String.valueOf(state));
        }

        @Override
        public void onMileageUnitChanged(int state) {
            super.onMileageUnitChanged(state);
            processer.put("instrument", "MileageUnit", String.valueOf(state));
        }

        @Override
        public void onLast50KmPowerConsumeChanged(double state) {
            super.onLast50KmPowerConsumeChanged(state);
            processer.put("instrument", "Last50KmPowerConsume", String.valueOf(state));
        }

        @Override
        public void onSpeedUnitChanged(int state) {
            super.onSpeedUnitChanged(state);
            processer.put("instrument", "SpeedUnit", String.valueOf(state));
        }

        @Override
        public void onBatteryPercentChanged(int state) {
            super.onBatteryPercentChanged(state);
            processer.put("instrument", "BatteryPercent", String.valueOf(state));
        }

        @Override
        public void onExternalChargePowerChanged(double state) {
            super.onExternalChargePowerChanged(state);
            processer.put("instrument", "ExternalChargePower", String.valueOf(state));
        }

        @Override
        public void onTravelTimeChanged(double state) {
            super.onTravelTimeChanged(state);
            processer.put("instrument", "TravelTime", String.valueOf(state));
        }

        @Override
        public void onPowerUnitChanged(int state) {
            super.onPowerUnitChanged(state);
            processer.put("instrument", "PowerUnit", String.valueOf(state));
        }

        @Override
        public void onAirHeatOilDisplayChanged(int state) {
            super.onAirHeatOilDisplayChanged(state);
            processer.put("instrument", "AirHeatOilDisplay", String.valueOf(state));
        }

        @Override
        public void onChargeDisplayChanged(int value) {
            super.onChargeDisplayChanged(value);
            processer.put("instrument", "ChargeDisplay", String.valueOf(value));
        }

        @Override
        public void onChargePercentChanged(int value) {
            super.onChargePercentChanged(value);
            processer.put("instrument", "ChargePercent", String.valueOf(value));
        }

        @Override
        public void onChargePowerChanged(double value) {
            super.onChargePowerChanged(value);
            processer.put("instrument", "ChargePower", String.valueOf(value));
        }

        @Override
        public void onChargeNoticeChanged(int value) {
            super.onChargeNoticeChanged(value);
            processer.put("instrument", "ChargeNotice", String.valueOf(value));
        }

        @Override
        public void onChargeRestTimeChanged(int[] resetTime) {
            super.onChargeRestTimeChanged(resetTime);
            processer.put("instrument", "ChargeRestTime", java.util.Arrays.toString(resetTime));
        }

        @Override
        public void onExpectChargeStateChanged(int value) {
            super.onExpectChargeStateChanged(value);
            processer.put("instrument", "ExpectChargeState", String.valueOf(value));
        }

        @Override
        public void onExpectChargeDisplayChanged(int value) {
            super.onExpectChargeDisplayChanged(value);
            processer.put("instrument", "ExpectChargeDisplay", String.valueOf(value));
        }

        @Override
        public void onACCIndicateLightStateChanged(int state) {
            super.onACCIndicateLightStateChanged(state);
            processer.put("instrument", "ACCIndicateLightState", String.valueOf(state));
        }

        @Override
        public void onACCIndicateLightColorChanged(int color) {
            super.onACCIndicateLightColorChanged(color);
            processer.put("instrument", "ACCIndicateLightColor", String.valueOf(color));
        }

        @Override
        public void onAccCruisingSpeedValueChanged(int value) {
            super.onAccCruisingSpeedValueChanged(value);
            processer.put("instrument", "AccCruisingSpeedValue", String.valueOf(value));
        }

        @Override
        public void onOilLevelAlarmIndicatorChanged(int value) {
            super.onOilLevelAlarmIndicatorChanged(value);
            processer.put("instrument", "OilLevelAlarmIndicator", String.valueOf(value));
        }

        @Override
        public void onOilLevelAlarmIndicatorColorChanged(int value) {
            super.onOilLevelAlarmIndicatorColorChanged(value);
            processer.put("instrument", "OilLevelAlarmIndicatorColor", String.valueOf(value));
        }

        @Override
        public void onOutCarTemperatureChanged(int value) {
            super.onOutCarTemperatureChanged(value);
            processer.put("instrument", "OutCarTemperature", String.valueOf(value));
        }

        @Override
        public void onLinkErrKeyTimeChanged(int flag, int value) {
            super.onLinkErrKeyTimeChanged(flag, value);
            processer.put("instrument", "LinkErrKeyTime", String.valueOf(flag) + ", " + String.valueOf(value));
        }

        @Override
        public void onSRSFaultWarningLightChanged(int value) {
            super.onSRSFaultWarningLightChanged(value);
            processer.put("instrument", "SRSFaultWarningLight", String.valueOf(value));
        }

        @Override
        public void onSRSFaultWarningLightColorChanged(int value) {
            super.onSRSFaultWarningLightColorChanged(value);
            processer.put("instrument", "SRSFaultWarningLightColor", String.valueOf(value));
        }

        @Override
        public void onABSFaultWarningLightChanged(int value) {
            super.onABSFaultWarningLightChanged(value);
            processer.put("instrument", "ABSFaultWarningLight", String.valueOf(value));
        }

        @Override
        public void onABSFaultWarningLightColorChanged(int value) {
            super.onABSFaultWarningLightColorChanged(value);
            processer.put("instrument", "ABSFaultWarningLightColor", String.valueOf(value));
        }

        @Override
        public void onBrakeSysFaultLightStateChanged(int value) {
            super.onBrakeSysFaultLightStateChanged(value);
            processer.put("instrument", "BrakeSysFaultLightState", String.valueOf(value));
        }

        @Override
        public void onBrakeSysFaultLightColorChanged(int value) {
            super.onBrakeSysFaultLightColorChanged(value);
            processer.put("instrument", "BrakeSysFaultLightColor", String.valueOf(value));
        }

        @Override
        public void onCoolantTempHighWarnLightStateChanged(int value) {
            super.onCoolantTempHighWarnLightStateChanged(value);
            processer.put("instrument", "CoolantTempHighWarnLightState", String.valueOf(value));
        }

        @Override
        public void onCoolantTempHighWarnLightColorChanged(int value) {
            super.onCoolantTempHighWarnLightColorChanged(value);
            processer.put("instrument", "CoolantTempHighWarnLightColor", String.valueOf(value));
        }

        @Override
        public void onELECParkingStateChanged(int value) {
            super.onELECParkingStateChanged(value);
            processer.put("instrument", "ELECParkingState", String.valueOf(value));
        }

        @Override
        public void onELECParkingColorChanged(int value) {
            super.onELECParkingColorChanged(value);
            processer.put("instrument", "ELECParkingColor", String.valueOf(value));
        }

        @Override
        public void onEngineFailWarnLightStateChanged(int value) {
            super.onEngineFailWarnLightStateChanged(value);
            processer.put("instrument", "EngineFailWarnLightState", String.valueOf(value));
        }

        @Override
        public void onEngineFailWarnLightColorChanged(int value) {
            super.onEngineFailWarnLightColorChanged(value);
            processer.put("instrument", "EngineFailWarnLightColor", String.valueOf(value));
        }

        @Override
        public void onESPFailWarnLightStateChanged(int value) {
            super.onESPFailWarnLightStateChanged(value);
            processer.put("instrument", "ESPFailWarnLightState", String.valueOf(value));
        }

        @Override
        public void onESPFailWarnLightColorChanged(int value) {
            super.onESPFailWarnLightColorChanged(value);
            processer.put("instrument", "ESPFailWarnLightColor", String.valueOf(value));
        }

        @Override
        public void onGPFIndicatorStateChanged(int value) {
            super.onGPFIndicatorStateChanged(value);
            processer.put("instrument", "GPFIndicatorState", String.valueOf(value));
        }

        @Override
        public void onGPFIndicatorColorChanged(int value) {
            super.onGPFIndicatorColorChanged(value);
            processer.put("instrument", "GPFIndicatorColor", String.valueOf(value));
        }

        @Override
        public void onLowFuelWarnLightStateChanged(int value) {
            super.onLowFuelWarnLightStateChanged(value);
            processer.put("instrument", "LowFuelWarnLightState", String.valueOf(value));
        }

        @Override
        public void onLowFuelWarnLightColorChanged(int value) {
            super.onLowFuelWarnLightColorChanged(value);
            processer.put("instrument", "LowFuelWarnLightColor", String.valueOf(value));
        }

        @Override
        public void onPressureWarnLightStateChanged(int value) {
            super.onPressureWarnLightStateChanged(value);
            processer.put("instrument", "PressureWarnLightState", String.valueOf(value));
        }

        @Override
        public void onPressureWarnLightColorChanged(int value) {
            super.onPressureWarnLightColorChanged(value);
            processer.put("instrument", "PressureWarnLightColor", String.valueOf(value));
        }

        @Override
        public void onMainAlarmIndicatorStateChanged(int value) {
            super.onMainAlarmIndicatorStateChanged(value);
            processer.put("instrument", "MainAlarmIndicatorState", String.valueOf(value));
        }

        @Override
        public void onMainAlarmIndicatorColorChanged(int value) {
            super.onMainAlarmIndicatorColorChanged(value);
            processer.put("instrument", "MainAlarmIndicatorColor", String.valueOf(value));
        }

        @Override
        public void onPressureSupplySysFailWarnLightStateChanged(int value) {
            super.onPressureSupplySysFailWarnLightStateChanged(value);
            processer.put("instrument", "PressureSupplySysFailWarnLightState", String.valueOf(value));
        }

        @Override
        public void onPressureSupplySysFailWarnLightColorChanged(int value) {
            super.onPressureSupplySysFailWarnLightColorChanged(value);
            processer.put("instrument", "PressureSupplySysFailWarnLightColor", String.valueOf(value));
        }

        @Override
        public void onSmartKeySysWarnLightStateChanged(int value) {
            super.onSmartKeySysWarnLightStateChanged(value);
            processer.put("instrument", "SmartKeySysWarnLightState", String.valueOf(value));
        }

        @Override
        public void onSmartKeySysWarnLightColorChanged(int value) {
            super.onSmartKeySysWarnLightColorChanged(value);
            processer.put("instrument", "SmartKeySysWarnLightColor", String.valueOf(value));
        }

        @Override
        public void onSteeringSYSFailWarnLightStateChanged(int value) {
            super.onSteeringSYSFailWarnLightStateChanged(value);
            processer.put("instrument", "SteeringSYSFailWarnLightState", String.valueOf(value));
        }

        @Override
        public void onSteeringSYSFailWarnLightColorChanged(int value) {
            super.onSteeringSYSFailWarnLightColorChanged(value);
            processer.put("instrument", "SteeringSYSFailWarnLightColor", String.valueOf(value));
        }

        @Override
        public void onTyrePressureSYSFailWarnLightStateChanged(int value) {
            super.onTyrePressureSYSFailWarnLightStateChanged(value);
            processer.put("instrument", "TyrePressureSYSFailWarnLightState", String.valueOf(value));
        }

        @Override
        public void onTyrePressureSYSFailWarnLightColorChanged(int value) {
            super.onTyrePressureSYSFailWarnLightColorChanged(value);
            processer.put("instrument", "TyrePressureSYSFailWarnLightColor", String.valueOf(value));
        }

        @Override
        public void onHeadlampFailWarnLightStateChanged(int value) {
            super.onHeadlampFailWarnLightStateChanged(value);
            processer.put("instrument", "HeadlampFailWarnLightState", String.valueOf(value));
        }

        @Override
        public void onHeadlampFailWarnLightColorChanged(int value) {
            super.onHeadlampFailWarnLightColorChanged(value);
            processer.put("instrument", "HeadlampFailWarnLightColor", String.valueOf(value));
        }

        @Override
        public void onCruiseCtrlIndicatorStateChanged(int value) {
            super.onCruiseCtrlIndicatorStateChanged(value);
            processer.put("instrument", "CruiseCtrlIndicatorState", String.valueOf(value));
        }

        @Override
        public void onCruiseCtrlIndicatorColorChanged(int value) {
            super.onCruiseCtrlIndicatorColorChanged(value);
            processer.put("instrument", "CruiseCtrlIndicatorColor", String.valueOf(value));
        }

        @Override
        public void onDishargeIndicatorStateChanged(int value) {
            super.onDishargeIndicatorStateChanged(value);
            processer.put("instrument", "DishargeIndicatorState", String.valueOf(value));
        }

        @Override
        public void onDischargeIndicatorColorChanged(int value) {
            super.onDischargeIndicatorColorChanged(value);
            processer.put("instrument", "DischargeIndicatorColor", String.valueOf(value));
        }

        @Override
        public void onDrivePowerLimitIndicatorStateChanged(int value) {
            super.onDrivePowerLimitIndicatorStateChanged(value);
            processer.put("instrument", "DrivePowerLimitIndicatorState", String.valueOf(value));
        }

        @Override
        public void onDrivePowerLimitIndicatorColorChanged(int value) {
            super.onDrivePowerLimitIndicatorColorChanged(value);
            processer.put("instrument", "DrivePowerLimitIndicatorColor", String.valueOf(value));
        }

        @Override
        public void onECOIndicatorStateChanged(int value) {
            super.onECOIndicatorStateChanged(value);
            processer.put("instrument", "ECOIndicatorState", String.valueOf(value));
        }

        @Override
        public void onECOIndicatorColorChanged(int value) {
            super.onECOIndicatorColorChanged(value);
            processer.put("instrument", "ECOIndicatorColor", String.valueOf(value));
        }

        @Override
        public void onEVIndicatorStateChanged(int value) {
            super.onEVIndicatorStateChanged(value);
            processer.put("instrument", "EVIndicatorState", String.valueOf(value));
        }

        @Override
        public void onEVIndicatorColorChanged(int value) {
            super.onEVIndicatorColorChanged(value);
            processer.put("instrument", "EVIndicatorColor", String.valueOf(value));
        }

        @Override
        public void onHEVIndicatorStateChanged(int value) {
            super.onHEVIndicatorStateChanged(value);
            processer.put("instrument", "HEVIndicatorState", String.valueOf(value));
        }

        @Override
        public void onHEVIndicatorColorChanged(int value) {
            super.onHEVIndicatorColorChanged(value);
            processer.put("instrument", "HEVIndicatorColor", String.valueOf(value));
        }

        @Override
        public void onLowPowerBatteryWarnLightStateChanged(int value) {
            super.onLowPowerBatteryWarnLightStateChanged(value);
            processer.put("instrument", "LowPowerBatteryWarnLightState", String.valueOf(value));
        }

        @Override
        public void onLowPowerBatteryWarnLightColorChanged(int value) {
            super.onLowPowerBatteryWarnLightColorChanged(value);
            processer.put("instrument", "LowPowerBatteryWarnLightColor", String.valueOf(value));
        }

        @Override
        public void onOKIndicatorStateChanged(int value) {
            super.onOKIndicatorStateChanged(value);
            processer.put("instrument", "OKIndicatorState", String.valueOf(value));
        }

        @Override
        public void onOKIndicatorColorChanged(int value) {
            super.onOKIndicatorColorChanged(value);
            processer.put("instrument", "OKIndicatorColor", String.valueOf(value));
        }

        @Override
        public void onPowerBatteryChargeConnectIndicatorStateChanged(int value) {
            super.onPowerBatteryChargeConnectIndicatorStateChanged(value);
            processer.put("instrument", "PowerBatteryChargeConnectIndicatorState", String.valueOf(value));
        }

        @Override
        public void onPowerBatteryChargeConnectIndicatorColorChanged(int value) {
            super.onPowerBatteryChargeConnectIndicatorColorChanged(value);
            processer.put("instrument", "PowerBatteryChargeConnectIndicatorColor", String.valueOf(value));
        }

        @Override
        public void onPowerBatteryHeatWarnLightStateChanged(int value) {
            super.onPowerBatteryHeatWarnLightStateChanged(value);
            processer.put("instrument", "PowerBatteryHeatWarnLightState", String.valueOf(value));
        }

        @Override
        public void onPowerBatteryHeatWarnLightColorChanged(int value) {
            super.onPowerBatteryHeatWarnLightColorChanged(value);
            processer.put("instrument", "PowerBatteryHeatWarnLightColor", String.valueOf(value));
        }

        @Override
        public void onPowerBatFailWarnLightStateChanged(int value) {
            super.onPowerBatFailWarnLightStateChanged(value);
            processer.put("instrument", "PowerBatFailWarnLightState", String.valueOf(value));
        }

        @Override
        public void onPowerBatFailWarnLightColorChanged(int value) {
            super.onPowerBatFailWarnLightColorChanged(value);
            processer.put("instrument", "PowerBatFailWarnLightColor", String.valueOf(value));
        }

        @Override
        public void onPowerSysFailWarnLightStateChanged(int value) {
            super.onPowerSysFailWarnLightStateChanged(value);
            processer.put("instrument", "PowerSysFailWarnLightState", String.valueOf(value));
        }

        @Override
        public void onPowerSysFailWarnLightColorChanged(int value) {
            super.onPowerSysFailWarnLightColorChanged(value);
            processer.put("instrument", "PowerSysFailWarnLightColor", String.valueOf(value));
        }

        @Override
        public void onSportIndicatorStateChanged(int value) {
            super.onSportIndicatorStateChanged(value);
            processer.put("instrument", "SportIndicatorState", String.valueOf(value));
        }

        @Override
        public void onSportIndicatorColorChanged(int value) {
            super.onSportIndicatorColorChanged(value);
            processer.put("instrument", "SportIndicatorColor", String.valueOf(value));
        }

        @Override
        public void onDischargeUiStateChanged(int value) {
            super.onDischargeUiStateChanged(value);
            processer.put("instrument", "DischargeUiState", String.valueOf(value));
        }

        @Override
        public void onDischargeModeChanged(int mode) {
            super.onDischargeModeChanged(mode);
            processer.put("instrument", "DischargeMode", String.valueOf(mode));
        }

        @Override
        public void onDischargeElecEnergyChanged(double value) {
            super.onDischargeElecEnergyChanged(value);
            processer.put("instrument", "DischargeElecEnergy", String.valueOf(value));
        }

        @Override
        public void onDirectionInfoChanged(int value) {
            super.onDirectionInfoChanged(value);
            processer.put("instrument", "DirectionInfo", String.valueOf(value));
        }

        @Override
        public void onTyrePressureCarTypeChanged(int type) {
            super.onTyrePressureCarTypeChanged(type);
            processer.put("instrument", "TyrePressureCarType", String.valueOf(type));
        }

        @Override
        public void onMileageValidFlagChanged(int value) {
            super.onMileageValidFlagChanged(value);
            processer.put("instrument", "MileageValidFlag", String.valueOf(value));
        }

        @Override
        public void onDashboardAlarmStateChanged(int value) {
            super.onDashboardAlarmStateChanged(value);
            processer.put("instrument", "DashboardAlarmState", String.valueOf(value));
        }

        @Override
        public void onCurrentJourneyDriveMileageChanged(double value) {
            super.onCurrentJourneyDriveMileageChanged(value);
            processer.put("instrument", "CurrentJourneyDriveMileage", String.valueOf(value));
        }

        @Override
        public void onCurrentJourneyDriveTimeChanged(double value) {
            super.onCurrentJourneyDriveTimeChanged(value);
            processer.put("instrument", "CurrentJourneyDriveTime", String.valueOf(value));
        }

        @Override
        public void onCurrentDriveInterFaceChanged(int state) {
            super.onCurrentDriveInterFaceChanged(state);
            processer.put("instrument", "CurrentDriveInterFace", String.valueOf(state));
        }

        @Override
        public void onOdometerDisplayChanged(int state) {
            super.onOdometerDisplayChanged(state);
            processer.put("instrument", "OdometerDisplay", String.valueOf(state));
        }

        @Override
        public void onAppCountdownHourChanged(int value) {
            super.onAppCountdownHourChanged(value);
            processer.put("instrument", "AppCountdownHour", String.valueOf(value));
        }

        @Override
        public void onAppCountdownMinuteChanged(int value) {
            super.onAppCountdownMinuteChanged(value);
            processer.put("instrument", "AppCountdownMinute", String.valueOf(value));
        }

        @Override
        public void onViewStatusChanged(int state) {
            super.onViewStatusChanged(state);
            processer.put("instrument", "ViewStatus", String.valueOf(state));
        }

        @Override
        public void onFirstMenuChanged(int value) {
            super.onFirstMenuChanged(value);
            processer.put("instrument", "FirstMenu", String.valueOf(value));
        }

        @Override
        public void onSecondMenuChanged(int value) {
            super.onSecondMenuChanged(value);
            processer.put("instrument", "SecondMenu", String.valueOf(value));
        }

        @Override
        public void onAirHeatingOilWarnChanged(int value) {
            super.onAirHeatingOilWarnChanged(value);
            processer.put("instrument", "AirHeatingOilWarn", String.valueOf(value));
        }

        @Override
        public void onChargeAppTimeOptionChanged(int state) {
            super.onChargeAppTimeOptionChanged(state);
            processer.put("instrument", "ChargeAppTimeOption", String.valueOf(state));
        }

        @Override
        public void on2in1AccDistanceChanged(int value) {
            super.on2in1AccDistanceChanged(value);
            processer.put("instrument", "2in1AccDistance", String.valueOf(value));
        }

        @Override
        public void on2in1AccTextPromptChanged(int value) {
            super.on2in1AccTextPromptChanged(value);
            processer.put("instrument", "2in1AccTextPrompt", String.valueOf(value));
        }

        @Override
        public void on2in1BodyPositionChanged(int value) {
            super.on2in1BodyPositionChanged(value);
            processer.put("instrument", "2in1BodyPosition", String.valueOf(value));
        }

        @Override
        public void onLineValueChanged(int flag, int value) {
            super.onLineValueChanged(flag, value);
            processer.put("instrument", "LineValue", String.valueOf(flag) + ", " + String.valueOf(value));
        }

        @Override
        public void on2in1AccWorkInterfaceChanged(int value) {
            super.on2in1AccWorkInterfaceChanged(value);
            processer.put("instrument", "2in1AccWorkInterface", String.valueOf(value));
        }

        @Override
        public void on2in1AccTimeDistanceChanged(int value) {
            super.on2in1AccTimeDistanceChanged(value);
            processer.put("instrument", "2in1AccTimeDistance", String.valueOf(value));
        }

        @Override
        public void onSoundFreqChanged(int state) {
            super.onSoundFreqChanged(state);
            processer.put("instrument", "SoundFreq", String.valueOf(state));
        }

        @Override
        public void on2in1FaultSmallLightIndicatorChanged(int value) {
            super.on2in1FaultSmallLightIndicatorChanged(value);
            processer.put("instrument", "2in1FaultSmallLightIndicator", String.valueOf(value));
        }

        @Override
        public void on2in1FaultSmallLightIndicatorColorChanged(int value) {
            super.on2in1FaultSmallLightIndicatorColorChanged(value);
            processer.put("instrument", "2in1FaultSmallLightIndicatorColor", String.valueOf(value));
        }

        @Override
        public void on2in1FaultFrontFogLightIndicatorChanged(int value) {
            super.on2in1FaultFrontFogLightIndicatorChanged(value);
            processer.put("instrument", "2in1FaultFrontFogLightIndicator", String.valueOf(value));
        }

        @Override
        public void on2in1FaultFrontFogLightIndicatorColorChanged(int value) {
            super.on2in1FaultFrontFogLightIndicatorColorChanged(value);
            processer.put("instrument", "2in1FaultFrontFogLightIndicatorColor", String.valueOf(value));
        }

        @Override
        public void on2in1FaultGrassIndicatorChanged(int value) {
            super.on2in1FaultGrassIndicatorChanged(value);
            processer.put("instrument", "2in1FaultGrassIndicator", String.valueOf(value));
        }

        @Override
        public void on2in1FaultGrassIndicatorColorChanged(int value) {
            super.on2in1FaultGrassIndicatorColorChanged(value);
            processer.put("instrument", "2in1FaultGrassIndicatorColor", String.valueOf(value));
        }

        @Override
        public void on2in1MenuStateChanged(int state) {
            super.on2in1MenuStateChanged(state);
            processer.put("instrument", "2in1MenuState", String.valueOf(state));
        }

        @Override
        public void onAppointmentHourChanged(int value) {
            super.onAppointmentHourChanged(value);
            processer.put("instrument", "AppointmentHour", String.valueOf(value));
        }

        @Override
        public void onAppointmentMinuteChanged(int value) {
            super.onAppointmentMinuteChanged(value);
            processer.put("instrument", "AppointmentMinute", String.valueOf(value));
        }

        @Override
        public void onInstrumentViewChanged(int value) {
            super.onInstrumentViewChanged(value);
            processer.put("instrument", "InstrumentView", String.valueOf(value));
        }

        @Override
        public void onFaultMuddyIndicatorChanged(int value) {
            super.onFaultMuddyIndicatorChanged(value);
            processer.put("instrument", "FaultMuddyIndicator", String.valueOf(value));
        }

        @Override
        public void onFaultMuddyIndicatorColorChanged(int value) {
            super.onFaultMuddyIndicatorColorChanged(value);
            processer.put("instrument", "FaultMuddyIndicatorColor", String.valueOf(value));
        }

        @Override
        public void onFaultNormalIndicatorChanged(int value) {
            super.onFaultNormalIndicatorChanged(value);
            processer.put("instrument", "FaultNormalIndicator", String.valueOf(value));
        }

        @Override
        public void onFaultNormalIndicatorColorChanged(int value) {
            super.onFaultNormalIndicatorColorChanged(value);
            processer.put("instrument", "FaultNormalIndicatorColor", String.valueOf(value));
        }

        @Override
        public void onFaultOilLifeDetectIndicatorChanged(int value) {
            super.onFaultOilLifeDetectIndicatorChanged(value);
            processer.put("instrument", "FaultOilLifeDetectIndicator", String.valueOf(value));
        }

        @Override
        public void onFaultOilLifeDetectIndicatorColorChanged(int value) {
            super.onFaultOilLifeDetectIndicatorColorChanged(value);
            processer.put("instrument", "FaultOilLifeDetectIndicatorColor", String.valueOf(value));
        }

        @Override
        public void onFaultSandIndicatorChanged(int value) {
            super.onFaultSandIndicatorChanged(value);
            processer.put("instrument", "FaultSandIndicator", String.valueOf(value));
        }

        @Override
        public void onFaultSandIndicatorColorChanged(int value) {
            super.onFaultSandIndicatorColorChanged(value);
            processer.put("instrument", "FaultSandIndicatorColor", String.valueOf(value));
        }

        @Override
        public void on50KmEneryConsumptionDisplayStateChanged(int value) {
            super.on50KmEneryConsumptionDisplayStateChanged(value);
            processer.put("instrument", "50KmEneryConsumptionDisplayState", String.valueOf(value));
        }

        @Override
        public void onAverageEneryConsumptionDisplayStateChanged(int value) {
            super.onAverageEneryConsumptionDisplayStateChanged(value);
            processer.put("instrument", "AverageEneryConsumptionDisplayState", String.valueOf(value));
        }

        @Override
        public void onAverageFuelConsumptionDisplayStateChanged(int value) {
            super.onAverageFuelConsumptionDisplayStateChanged(value);
            processer.put("instrument", "AverageFuelConsumptionDisplayState", String.valueOf(value));
        }

        @Override
        public void onFuelConsumptionDisplayStateChanged(int value) {
            super.onFuelConsumptionDisplayStateChanged(value);
            processer.put("instrument", "FuelConsumptionDisplayState", String.valueOf(value));
        }

        @Override
        public void onInstantFuelConsumptionDisplayStateChanged(int value) {
            super.onInstantFuelConsumptionDisplayStateChanged(value);
            processer.put("instrument", "InstantFuelConsumptionDisplayState", String.valueOf(value));
        }

        @Override
        public void onInstantFuelConsumptionUnitChanged(int value) {
            super.onInstantFuelConsumptionUnitChanged(value);
            processer.put("instrument", "InstantFuelConsumptionUnit", String.valueOf(value));
        }

        @Override
        public void onWaterTempMeterPercentChanged(double value) {
            super.onWaterTempMeterPercentChanged(value);
            processer.put("instrument", "WaterTempMeterPercent", String.valueOf(value));
        }

        @Override
        public void onAirHeatingDisplayStateChanged(int value) {
            super.onAirHeatingDisplayStateChanged(value);
            processer.put("instrument", "AirHeatingDisplayState", String.valueOf(value));
        }

        @Override
        public void onDirectTypePressDisplayStateChanged(int value) {
            super.onDirectTypePressDisplayStateChanged(value);
            processer.put("instrument", "DirectTypePressDisplayState", String.valueOf(value));
        }

        @Override
        public void on50KmFuelConsumptionDisplayStateChanged(int value) {
            super.on50KmFuelConsumptionDisplayStateChanged(value);
            processer.put("instrument", "50KmFuelConsumptionDisplayState", String.valueOf(value));
        }

        @Override
        public void onEnergyDisplayChanged(int status) {
            super.onEnergyDisplayChanged(status);
            processer.put("instrument", "EnergyDisplay", String.valueOf(status));
        }

        @Override
        public void onFaultIndicatorChanged(int indicatorType, int value) {
            super.onFaultIndicatorChanged(indicatorType, value);
            processer.put("instrument", "FaultIndicator", String.valueOf(indicatorType) + ", " + String.valueOf(value));
        }

        @Override
        public void onFaultIndicatorColorChanged(int indicatorType, int value) {
            super.onFaultIndicatorColorChanged(indicatorType, value);
            processer.put("instrument", "FaultIndicatorColor", String.valueOf(indicatorType) + ", " + String.valueOf(value));
        }

        @Override
        public void onFuelLowAlarmChanged(int value) {
            super.onFuelLowAlarmChanged(value);
            processer.put("instrument", "FuelLowAlarm", String.valueOf(value));
        }

        @Override
        public void onError(int errCode, String errMessage) {
            super.onError(errCode, errMessage);
            processer.put("instrument", "Error", String.valueOf(errCode) + ", " + errMessage);
        }

        @Override
        public void onDataEventChanged(int eventType, BYDAutoEventValue eventValue) {
            super.onDataEventChanged(eventType, eventValue);
            processer.put("instrument", "DataEvent", String.valueOf(eventType) + ", " + String.valueOf(eventValue));
        }
    };

//    AbsBYDAutoLightListener

//    AbsBYDAutoMultimediaListener

    AbsBYDAutoPanoramaListener panoramaListener = new AbsBYDAutoPanoramaListener() {
        @Override
        public void onPanoWorkStateChanged(int mode) {
            super.onPanoWorkStateChanged(mode);
            processer.put("panorama", "PanoWorkState", String.valueOf(mode));
        }

        @Override
        public void onPanOutputStateChanged(int mode) {
            super.onPanOutputStateChanged(mode);
            processer.put("panorama", "PanOutputState", String.valueOf(mode));
        }

        @Override
        public void onBackLineConfigChanged(int mode) {
            super.onBackLineConfigChanged(mode);
            processer.put("panorama", "BackLineConfig", String.valueOf(mode));
        }

        @Override
        public void onPanoramaOnlineStateChanged(int value) {
            super.onPanoramaOnlineStateChanged(value);
            processer.put("panorama", "PanoramaOnlineState", String.valueOf(value));
        }

        @Override
        public void onPanoRotationChanged(int value) {
            super.onPanoRotationChanged(value);
            processer.put("panorama", "PanoRotation", String.valueOf(value));
        }

        @Override
        public void onDisplayModeChanged(int mode) {
            super.onDisplayModeChanged(mode);
            processer.put("panorama", "DisplayMode", String.valueOf(mode));
        }

        @Override
        public void onError(int errCode, String errMessage) {
            super.onError(errCode, errMessage);
            processer.put("panorama", "Error", String.valueOf(errCode) + ", " + errMessage);
        }

        @Override
        public void onDataEventChanged(int eventType, BYDAutoEventValue eventValue) {
            super.onDataEventChanged(eventType, eventValue);
            processer.put("panorama", "DataEvent", String.valueOf(eventType) + ", " + String.valueOf(eventValue));
        }
    };

//    AbsBYDAutoPM2p5Listener

    AbsBYDAutoRadarListener radarListener = new AbsBYDAutoRadarListener() {
        @Override
        public void onRadarProbeStateChanged(int area, int state) {
            super.onRadarProbeStateChanged(area, state);
            processer.put("radar", "RadarProbeState", String.valueOf(area) + ", " + String.valueOf(state));
        }

        @Override
        public void onReverseRadarSwitchStateChanged(int state) {
            super.onReverseRadarSwitchStateChanged(state);
            processer.put("radar", "ReverseRadarSwitchState", String.valueOf(state));
        }

        @Override
        public void onError(int errCode, String errMessage) {
            super.onError(errCode, errMessage);
            processer.put("radar", "Error", String.valueOf(errCode) + ", " + errMessage);
        }

        @Override
        public void onDataEventChanged(int eventType, BYDAutoEventValue eventValue) {
            super.onDataEventChanged(eventType, eventValue);
            processer.put("radar", "DataEvent", String.valueOf(eventType) + ", " + String.valueOf(eventValue));
        }
    };

//    AbsBYDAutoSafetyBeltListener

    AbsBYDAutoSensorListener sensorListener = new AbsBYDAutoSensorListener() {
        @Override
        public void onTemperatureSensorValueChanged(double value) {
            super.onTemperatureSensorValueChanged(value);
            processer.put("sensor", "TemperatureSensorValue", String.valueOf(value));
        }

        @Override
        public void onHumiditySensorValueChanged(double value) {
            super.onHumiditySensorValueChanged(value);
            processer.put("sensor", "HumiditySensorValue", String.valueOf(value));
        }

        @Override
        public void onLightIntensityChanged(int value) {
            super.onLightIntensityChanged(value);
            processer.put("sensor", "LightIntensity", String.valueOf(value));
        }

        @Override
        public void onSlopeValueChanged(int value) {
            super.onSlopeValueChanged(value);
            processer.put("sensor", "SlopeValue", String.valueOf(value));
        }

        @Override
        public void onAccSensorDataChanged(byte[] data) {
            super.onAccSensorDataChanged(data);
            processer.put("sensor", "AccSensorData", java.util.Arrays.toString(data));
        }

        @Override
        public void onError(int errCode, String errMessage) {
            super.onError(errCode, errMessage);
            processer.put("sensor", "Error", String.valueOf(errCode) + ", " + errMessage);
        }

        @Override
        public void onDataEventChanged(int eventType, BYDAutoEventValue eventValue) {
            super.onDataEventChanged(eventType, eventValue);
            processer.put("sensor", "DataEvent", String.valueOf(eventType) + ", " + String.valueOf(eventValue));
        }
    };

    AbsBYDAutoSettingListener settingListener = new AbsBYDAutoSettingListener() {
        @Override
        public void onACBTWindSwitchChanged(int state) {
            super.onACBTWindSwitchChanged(state);
            processer.put("setting", "ACBTWindSwitch", String.valueOf(state));
        }

        @Override
        public void onACTunnelCycleSwitchChanged(int state) {
            super.onACTunnelCycleSwitchChanged(state);
            processer.put("setting", "ACTunnelCycleSwitch", String.valueOf(state));
        }

        @Override
        public void onACPauseCycleSwitchChanged(int state) {
            super.onACPauseCycleSwitchChanged(state);
            processer.put("setting", "ACPauseCycleSwitch", String.valueOf(state));
        }

        @Override
        public void onACAutoAirModeChanged(int state) {
            super.onACAutoAirModeChanged(state);
            processer.put("setting", "ACAutoAirMode", String.valueOf(state));
        }

        @Override
        public void onEnergyFeedbackStrengthChanged(int level) {
            super.onEnergyFeedbackStrengthChanged(level);
            processer.put("setting", "EnergyFeedbackStrength", String.valueOf(level));
        }

        @Override
        public void onSOCTargetRangeChanged(int state) {
            super.onSOCTargetRangeChanged(state);
            processer.put("setting", "SOCTargetRange", String.valueOf(state));
        }

        @Override
        public void onChargingPortSwitchChanged(int state) {
            super.onChargingPortSwitchChanged(state);
            processer.put("setting", "ChargingPortSwitch", String.valueOf(state));
        }

        @Override
        public void onSteerAssisModeChanged(int state) {
            super.onSteerAssisModeChanged(state);
            processer.put("setting", "SteerAssisMode", String.valueOf(state));
        }

        @Override
        public void onRearViewMirrorFlipSwitchChanged(int state) {
            super.onRearViewMirrorFlipSwitchChanged(state);
            processer.put("setting", "RearViewMirrorFlipSwitch", String.valueOf(state));
        }

        @Override
        public void onDriverSeatAutoReturnSwitchChanged(int state) {
            super.onDriverSeatAutoReturnSwitchChanged(state);
            processer.put("setting", "DriverSeatAutoReturnSwitch", String.valueOf(state));
        }

        @Override
        public void onSteerPositionAutoReturnSwitchChanged(int state) {
            super.onSteerPositionAutoReturnSwitchChanged(state);
            processer.put("setting", "SteerPositionAutoReturnSwitch", String.valueOf(state));
        }

        @Override
        public void onPM25PowerSwitchChanged(int state) {
            super.onPM25PowerSwitchChanged(state);
            processer.put("setting", "PM25PowerSwitch", String.valueOf(state));
        }

        @Override
        public void onPM25SwitchCheckChanged(int state) {
            super.onPM25SwitchCheckChanged(state);
            processer.put("setting", "PM25SwitchCheck", String.valueOf(state));
        }

        @Override
        public void onPM25TimeCheckChanged(int state) {
            super.onPM25TimeCheckChanged(state);
            processer.put("setting", "PM25TimeCheck", String.valueOf(state));
        }

        @Override
        public void onControlWindowSwitchChanged(int state) {
            super.onControlWindowSwitchChanged(state);
            processer.put("setting", "ControlWindowSwitch", String.valueOf(state));
        }

        @Override
        public void onLockCarRiseWindowChanged(int state) {
            super.onLockCarRiseWindowChanged(state);
            processer.put("setting", "LockCarRiseWindow", String.valueOf(state));
        }

        @Override
        public void onBackHomeLightDelayValueChanged(int value) {
            super.onBackHomeLightDelayValueChanged(value);
            processer.put("setting", "BackHomeLightDelayValue", String.valueOf(value));
        }

        @Override
        public void onLeftHomeLightDelayValueChanged(int value) {
            super.onLeftHomeLightDelayValueChanged(value);
            processer.put("setting", "LeftHomeLightDelayValue", String.valueOf(value));
        }

        @Override
        public void onLockOffDoorChanged(int state) {
            super.onLockOffDoorChanged(state);
            processer.put("setting", "LockOffDoor", String.valueOf(state));
        }

        @Override
        public void onRemoteControlUpwindowStateChanged(int state) {
            super.onRemoteControlUpwindowStateChanged(state);
            processer.put("setting", "RemoteControlUpwindowState", String.valueOf(state));
        }

        @Override
        public void onMicroSwitchLockWindowStateChanged(int state) {
            super.onMicroSwitchLockWindowStateChanged(state);
            processer.put("setting", "MicroSwitchLockWindowState", String.valueOf(state));
        }

        @Override
        public void onRearAcOnlineStateChanged(int state) {
            super.onRearAcOnlineStateChanged(state);
            processer.put("setting", "RearAcOnlineState", String.valueOf(state));
        }

        @Override
        public void onBackDoorElectricModeChanged(int mode) {
            super.onBackDoorElectricModeChanged(mode);
            processer.put("setting", "BackDoorElectricMode", String.valueOf(mode));
        }

        @Override
        public void onFeatureChanged(String feature, int ifHas) {
            super.onFeatureChanged(feature, ifHas);
            processer.put("setting", "Feature", feature + ", " + String.valueOf(ifHas));
        }

        @Override
        public void onOverspeedLockStateChanged(int state) {
            super.onOverspeedLockStateChanged(state);
            processer.put("setting", "OverspeedLockState", String.valueOf(state));
        }

        @Override
        public void onLanguageChanged(int value) {
            super.onLanguageChanged(value);
            processer.put("setting", "Language", String.valueOf(value));
        }

        @Override
        public void onSafeWarnStateChanged(int state) {
            super.onSafeWarnStateChanged(state);
            processer.put("setting", "SafeWarnState", String.valueOf(state));
        }

        @Override
        public void onMaintainRemindStateChanged(int state) {
            super.onMaintainRemindStateChanged(state);
            processer.put("setting", "MaintainRemindState", String.valueOf(state));
        }

        @Override
        public void onMicroSwitchUnlockWindowStateChanged(int state) {
            super.onMicroSwitchUnlockWindowStateChanged(state);
            processer.put("setting", "MicroSwitchUnlockWindowState", String.valueOf(state));
        }

        @Override
        public void onAutoExternalRearMirrorFollowUpSwitchChanged(int state) {
            super.onAutoExternalRearMirrorFollowUpSwitchChanged(state);
            processer.put("setting", "AutoExternalRearMirrorFollowUpSwitch", String.valueOf(state));
        }

        @Override
        public void onError(int errCode, String errMessage) {
            super.onError(errCode, errMessage);
            processer.put("setting", "Error", String.valueOf(errCode) + ", " + errMessage);
        }

        @Override
        public void onDataEventChanged(int eventType, BYDAutoEventValue eventValue) {
            super.onDataEventChanged(eventType, eventValue);
            processer.put("setting", "DataEvent", String.valueOf(eventType) + ", " + String.valueOf(eventValue));
        }
    };

    AbsBYDAutoSpeedListener speedListener = new AbsBYDAutoSpeedListener() {
        @Override
        public void onSpeedChanged(double value) {
            super.onSpeedChanged(value);
            processer.put("speed", "Speed", String.valueOf(value));

            int power = autoEngineDevice.getEnginePower();
            processer.put("speed", "Power", String.valueOf(power));
        }

        @Override
        public void onAccelerateDeepnessChanged(int value) {
            super.onAccelerateDeepnessChanged(value);
            processer.put("speed", "AccelerateDeepness", String.valueOf(value));
        }

        @Override
        public void onBrakeDeepnessChanged(int value) {
            super.onBrakeDeepnessChanged(value);
            processer.put("speed", "BrakeDeepness", String.valueOf(value));
        }

        @Override
        public void onError(int errCode, String errMessage) {
            super.onError(errCode, errMessage);
            processer.put("speed", "Error", String.valueOf(errCode) + ", " + errMessage);
        }

        @Override
        public void onDataEventChanged(int eventType, BYDAutoEventValue eventValue) {
            super.onDataEventChanged(eventType, eventValue);
            processer.put("speed", "DataEvent", String.valueOf(eventType) + ", " + String.valueOf(eventValue));
        }
    };

    AbsBYDAutoStatisticListener statisticListener = new AbsBYDAutoStatisticListener() {
        @Override
        public void onTotalMileageValueChanged(int value) {
            super.onTotalMileageValueChanged(value);
            processer.put("statistic", "TotalMileageValue", String.valueOf(value));
        }

        @Override
        public void onTotalFuelConChanged(double value) {
            super.onTotalFuelConChanged(value);
            processer.put("statistic", "TotalFuelCon", String.valueOf(value));
        }

        @Override
        public void onTotalElecConChanged(double value) {
            super.onTotalElecConChanged(value);
            processer.put("statistic", "TotalElecCon", String.valueOf(value));
        }

        @Override
        public void onDrivingTimeChanged(double value) {
            super.onDrivingTimeChanged(value);
            processer.put("statistic", "DrivingTime", String.valueOf(value));
        }

        @Override
        public void onLastFuelConPHMChanged(double value) {
            super.onLastFuelConPHMChanged(value);
            processer.put("statistic", "LastFuelConPHM", String.valueOf(value));
        }

        @Override
        public void onTotalFuelConPHMChanged(double value) {
            super.onTotalFuelConPHMChanged(value);
            processer.put("statistic", "TotalFuelConPHM", String.valueOf(value));
        }

        @Override
        public void onLastElecConPHMChanged(double value) {
            super.onLastElecConPHMChanged(value);
            processer.put("statistic", "LastElecConPHM", String.valueOf(value));
        }

        @Override
        public void onTotalElecConPHMChanged(double value) {
            super.onTotalElecConPHMChanged(value);
            processer.put("statistic", "TotalElecConPHM", String.valueOf(value));
        }

        @Override
        public void onElecDrivingRangeChanged(int value) {
            super.onElecDrivingRangeChanged(value);
            processer.put("statistic", "ElecDrivingRange", String.valueOf(value));
        }

        @Override
        public void onFuelDrivingRangeChanged(int value) {
            super.onFuelDrivingRangeChanged(value);
            processer.put("statistic", "FuelDrivingRange", String.valueOf(value));
        }

        @Override
        public void onFuelPercentageChanged(int value) {
            super.onFuelPercentageChanged(value);
            processer.put("statistic", "FuelPercentage", String.valueOf(value));
        }

        @Override
        public void onElecPercentageChanged(double value) {
            super.onElecPercentageChanged(value);
            processer.put("statistic", "ElecPercentage", String.valueOf(value));
        }

        @Override
        public void onKeyBatteryLevelChanged(int value) {
            super.onKeyBatteryLevelChanged(value);
            processer.put("statistic", "KeyBatteryLevel", String.valueOf(value));
        }

        @Override
        public void onEVMileageValueChanged(int value) {
            super.onEVMileageValueChanged(value);
            processer.put("statistic", "EVMileageValue", String.valueOf(value));
        }

        @Override
        public void onError(int errCode, String errMessage) {
            super.onError(errCode, errMessage);
            processer.put("statistic", "Error", String.valueOf(errCode) + ", " + errMessage);
        }

        @Override
        public void onDataEventChanged(int eventType, BYDAutoEventValue eventValue) {
            super.onDataEventChanged(eventType, eventValue);
            processer.put("statistic", "DataEvent", String.valueOf(eventType) + ", " + String.valueOf(eventValue));
        }

        @Override
        public void onHEVMileageValueChanged(int value) {
            super.onHEVMileageValueChanged(value);
            processer.put("statistic", "HEVMileageValue", String.valueOf(value));
        }

        @Override
        public void onWaterTemperatureChanged(int value) {
            super.onWaterTemperatureChanged(value);
            processer.put("statistic", "WaterTemperature", String.valueOf(value));
        }

        @Override
        public void onInstantElecConChanged(double value) {
            super.onInstantElecConChanged(value);
            processer.put("statistic", "InstantElecCon", String.valueOf(value));
        }

        @Override
        public void onInstantFuelConChanged(double value) {
            super.onInstantFuelConChanged(value);
            processer.put("statistic", "InstantFuelCon", String.valueOf(value));
        }
    };

//    AbsBYDAutoTimeListener

    AbsBYDAutoTyreListener tyreListener = new AbsBYDAutoTyreListener() {
        @Override
        public void onTyreSystemStateChanged(int state) {
            super.onTyreSystemStateChanged(state);
            processer.put("tyre", "TyreSystemState", String.valueOf(state));
        }

        @Override
        public void onTyreTemperatureStateChanged(int state) {
            super.onTyreTemperatureStateChanged(state);
            processer.put("tyre", "TyreTemperatureState", String.valueOf(state));
        }

        @Override
        public void onTyreBatteryStateChanged(int state) {
            super.onTyreBatteryStateChanged(state);
            processer.put("tyre", "TyreBatteryState", String.valueOf(state));
        }

        @Override
        public void onTyreAirLeakStateChanged(int area, int state) {
            super.onTyreAirLeakStateChanged(area, state);
            processer.put("tyre", "TyreAirLeakState", String.valueOf(area) + ", " + String.valueOf(state));
        }

        @Override
        public void onTyreSignalStateChanged(int area, int state) {
            super.onTyreSignalStateChanged(area, state);
            processer.put("tyre", "TyreSignalState", String.valueOf(area) + ", " + String.valueOf(state));
        }

        @Override
        public void onTyrePressureStateChanged(int area, int state) {
            super.onTyrePressureStateChanged(area, state);
            processer.put("tyre", "TyrePressureState", String.valueOf(area) + ", " + String.valueOf(state));
        }

        @Override
        public void onTyrePressureValueChanged(int area, int value) {
            super.onTyrePressureValueChanged(area, value);
            processer.put("tyre", "TyrePressureValue", String.valueOf(area) + ", " + String.valueOf(value));
        }

        @Override
        public void onError(int errCode, String errMessage) {
            super.onError(errCode, errMessage);
            processer.put("tyre", "Error", String.valueOf(errCode) + ", " + errMessage);
        }

        @Override
        public void onDataEventChanged(int eventType, BYDAutoEventValue eventValue) {
            super.onDataEventChanged(eventType, eventValue);
            processer.put("tyre", "DataEvent", String.valueOf(eventType) + ", " + String.valueOf(eventValue));
        }
    };

}
