package com.toddmo.bydmate.collector;

import android.content.Context;
import android.hardware.bydauto.BYDAutoEventValue;
import android.hardware.bydauto.BYDAutoFeatureIds;
import android.hardware.bydauto.ac.AbsBYDAutoAcListener;
import android.hardware.bydauto.ac.BYDAutoAcDevice;
import android.hardware.bydauto.bodywork.AbsBYDAutoBodyworkListener;
import android.hardware.bydauto.bodywork.BYDAutoBodyworkDevice;
import android.hardware.bydauto.charging.AbsBYDAutoChargingListener;
import android.hardware.bydauto.charging.BYDAutoChargingDevice;
import android.hardware.bydauto.energy.AbsBYDAutoEnergyListener;
import android.hardware.bydauto.energy.BYDAutoEnergyDevice;
import android.hardware.bydauto.engine.AbsBYDAutoEngineListener;
import android.hardware.bydauto.engine.BYDAutoEngineDevice;
import android.hardware.bydauto.gearbox.AbsBYDAutoGearboxListener;
import android.hardware.bydauto.gearbox.BYDAutoGearboxDevice;
import android.hardware.bydauto.instrument.AbsBYDAutoInstrumentListener;
import android.hardware.bydauto.instrument.BYDAutoInstrumentDevice;
import android.hardware.bydauto.setting.AbsBYDAutoSettingListener;
import android.hardware.bydauto.setting.BYDAutoSettingDevice;
import android.hardware.bydauto.speed.AbsBYDAutoSpeedListener;
import android.hardware.bydauto.speed.BYDAutoSpeedDevice;
import android.hardware.bydauto.statistic.AbsBYDAutoStatisticListener;
import android.hardware.bydauto.statistic.BYDAutoStatisticDevice;
import android.hardware.bydauto.tyre.AbsBYDAutoTyreListener;
import android.hardware.bydauto.tyre.BYDAutoTyreDevice;
import android.util.Log;

import androidx.annotation.Keep;

import com.toddmo.bydmate.client.helper.AutoStatisticDeviceHelper;
import com.toddmo.bydmate.client.helper.BydApi29Helper;
import com.toddmo.bydmate.client.helper.ChargingDeviceHelper;
import com.toddmo.bydmate.client.utils.KLog;
import com.toddmo.bydmate.client.utils.StringUtils;

import java.time.Instant;
import java.util.Arrays;

public class DataCollector {
    private Context mContext;

    private final static String TAG = DataCollector.class.getSimpleName();

    private BYDAutoEngineDevice engineDevice;
    private BYDAutoSpeedDevice speedDevice;
    private BYDAutoStatisticDevice statisticDevice;
    private BYDAutoEnergyDevice energyDevice;
    private BYDAutoGearboxDevice gearboxDevice;
    private BYDAutoAcDevice bydAutoAcDevice;
    private BYDAutoChargingDevice chargingDevice;
    private BYDAutoTyreDevice tyreDevice;
    private BYDAutoBodyworkDevice bodyworkDevice;
    private BYDAutoSettingDevice settingDevice;
    private BYDAutoInstrumentDevice instrumentDevice;

    public DataCollector(Context context) {
        mContext = context;
        mDataCache = new DataProcesser(mContext);
    }

    public void initialize() {



//        if (ContextCompat.checkSelfPermission(mContext, BydManifest.permission.BYDAUTO_BODYWORK_COMMON) != PackageManager.PERMISSION_GRANTED) {
//            Toast.makeText(mContext, "权限不足", Toast.LENGTH_SHORT).show();
//            return;
//        }
        bodyworkDevice = BYDAutoBodyworkDevice.getInstance(mContext);


//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                KLog.i(String.format("deadloop running"));
//                while (true) {
//                    int value = bodyworkDevice.getAutoSystemState();
//                    KLog.i(String.format("value = %d", value));
//                    try { Thread.sleep(1000); } catch (Exception e) {}
//                }
//            }
//        }).start();


        statisticDevice = BYDAutoStatisticDevice.getInstance(mContext);
        speedDevice = BYDAutoSpeedDevice.getInstance(mContext);
        energyDevice = BYDAutoEnergyDevice.getInstance(mContext);
        engineDevice = BYDAutoEngineDevice.getInstance(mContext);
        bydAutoAcDevice = BYDAutoAcDevice.getInstance(mContext);
        gearboxDevice = BYDAutoGearboxDevice.getInstance(mContext);
        chargingDevice = BYDAutoChargingDevice.getInstance(mContext);
        tyreDevice = BYDAutoTyreDevice.getInstance(mContext);
        settingDevice = BYDAutoSettingDevice.getInstance(mContext);
        instrumentDevice = BYDAutoInstrumentDevice.getInstance(mContext);



        registerDevice();
    }

    private void registerDevice() {
        statisticDevice.registerListener(absBYDAutoStatisticListener);
        bodyworkDevice.registerListener(absBYDAutoBodyworkListener);
        speedDevice.registerListener(absBYDAutoSpeedListener);
        energyDevice.registerListener(absBYDAutoEnergyListener);
        engineDevice.registerListener(absBYDAutoEngineListener);
        bydAutoAcDevice.registerListener(absBYDAutoAcListener);
        gearboxDevice.registerListener(absBYDAutoGearboxListener);
        chargingDevice.registerListener(absBYDAutoChargingListener);
        tyreDevice.registerListener(absBYDAutoTyreListener);
        settingDevice.registerListener(absBYDAutoSettingListener);
        instrumentDevice.registerListener(instrumentListener);
    }

    private void unregisterDevice() {
        statisticDevice.unregisterListener(absBYDAutoStatisticListener);
        bodyworkDevice.unregisterListener(absBYDAutoBodyworkListener);
        speedDevice.unregisterListener(absBYDAutoSpeedListener);
        energyDevice.unregisterListener(absBYDAutoEnergyListener);
        engineDevice.unregisterListener(absBYDAutoEngineListener);
        bydAutoAcDevice.unregisterListener(absBYDAutoAcListener);
        gearboxDevice.unregisterListener(absBYDAutoGearboxListener);
        chargingDevice.unregisterListener(absBYDAutoChargingListener);
        tyreDevice.unregisterListener(absBYDAutoTyreListener);
        settingDevice.unregisterListener(absBYDAutoSettingListener);
        instrumentDevice.unregisterListener(instrumentListener);
    }

//    Map<String, String> mDataCache = new HashMap<>();
    DataProcesser mDataCache = null;

    private final AbsBYDAutoSettingListener absBYDAutoSettingListener = new AbsBYDAutoSettingListener() {
        /**
         * 能量回馈强度
         * @param level
         */
        @Override
        public void onEnergyFeedbackStrengthChanged(int level) {
            super.onEnergyFeedbackStrengthChanged(level);
            KLog.i("onEnergyFeedbackStrengthChanged " + level);
            if (level == BYDAutoSettingDevice.SET_DR_ENERGY_FB_STANDARD) {
                mDataCache.put("EnergyFeedback", "标准");
            } else if (level == BYDAutoSettingDevice.SET_DR_ENERGY_FB_LARGE) {
                mDataCache.put("EnergyFeedback", "较大");
            }
        }
    };

    private final AbsBYDAutoBodyworkListener absBYDAutoBodyworkListener = new AbsBYDAutoBodyworkListener() {
        /**
         * 电源档位
         * @param level
         */
        @Override
        public void onPowerLevelChanged(int level) {
            super.onPowerLevelChanged(level);
//            dataHolder.setPowerLevel(StringUtil.getPowerLevelName(level));
            KLog.i("onPowerLevelChanged " + level);
            mDataCache.put("PowerLevel", StringUtils.getPowerLevelName(level));
        }

    };

    private final AbsBYDAutoTyreListener absBYDAutoTyreListener = new AbsBYDAutoTyreListener() {
        /**
         *
         * @param area
         * @param state
         */
        @Override
        public void onTyrePressureStateChanged(int area, int state) {
            super.onTyrePressureStateChanged(area, state);
//            if (supportTyreValue) {
//                return;
//            }
            KLog.i("onTyrePressureStateChanged " + area + ", " + state);
            String tyreStateName = StringUtils.getTyreStateName(state);
            if (area == BYDAutoTyreDevice.TYRE_COMMAND_AREA_LEFT_FRONT) {
                mDataCache.put("TyrePreLeftFront", tyreStateName);
            }
            if (area == BYDAutoTyreDevice.TYRE_COMMAND_AREA_RIGHT_FRONT) {
                mDataCache.put("TyrePreRightFront", tyreStateName);
            }
            if (area == BYDAutoTyreDevice.TYRE_COMMAND_AREA_LEFT_REAR) {
                mDataCache.put("TyrePreLeftRear", tyreStateName);
            }
            if (area == BYDAutoTyreDevice.TYRE_COMMAND_AREA_RIGHT_REAR) {
                mDataCache.put("TyrePreRightRear", tyreStateName);
            }
        }

        /**
         * 胎压值变化（只有胎压报警的车型无此数据）
         * @param area
         * @param value
         */
        @Override
        public void onTyrePressureValueChanged(int area, int value) {
            super.onTyrePressureValueChanged(area, value);
//            if (!supportTyreValue) {
//                return;
//            }
            KLog.i("onTyrePressureValueChanged " + area + ", " + value);
            if (area == BYDAutoTyreDevice.TYRE_COMMAND_AREA_LEFT_FRONT) {
                mDataCache.put("TyrePreRightRear", String.valueOf(value));
            }
            if (area == BYDAutoTyreDevice.TYRE_COMMAND_AREA_RIGHT_FRONT) {
                mDataCache.put("TyrePreRightFront", String.valueOf(value));
            }
            if (area == BYDAutoTyreDevice.TYRE_COMMAND_AREA_LEFT_REAR) {
                mDataCache.put("TyrePreLeftRear", String.valueOf(value));
            }
            if (area == BYDAutoTyreDevice.TYRE_COMMAND_AREA_RIGHT_REAR) {
                mDataCache.put("TyrePreRightRear", String.valueOf(value));
            }
        }

        @Override
        public void onDataEventChanged(int eventType, BYDAutoEventValue eventValue) {
            super.onDataEventChanged(eventType, eventValue);
            Log.e(TAG, "onDataEventChanged 轮胎信息："
                    + eventType + " " + eventValue.intValue + " "
                    + eventValue.floatValue + " " + eventValue.doubleValue);
        }
    };

    private final AbsBYDAutoGearboxListener absBYDAutoGearboxListener = new AbsBYDAutoGearboxListener() {
        /**
         * 监听自动变速箱档位变化
         * @param level
         */
        @Override
        public void onGearboxAutoModeTypeChanged(int level) {
            super.onGearboxAutoModeTypeChanged(level);
            KLog.i("onGearboxAutoModeTypeChanged " + level);
            mDataCache.put("CurrentGearboxLevel", StringUtils.getGearboxLevelName(level));
        }
    };

    private final AbsBYDAutoChargingListener absBYDAutoChargingListener = new AbsBYDAutoChargingListener() {
        /**
         * 充电功率变化监听
         *
         * @param value
         */
        @Override
        public void onChargingPowerChanged(double value) {
            super.onChargingPowerChanged(value);
            KLog.i("onChargingPowerChanged " + value);
            mDataCache.put("ChargePower", String.valueOf(value));
        }

        /**
         * 获取充满电剩余时间
         *
         * @param hour
         * @param min
         */
        @Override
        public void onChargingRestTimeChanged(int hour, int min) {
            super.onChargingRestTimeChanged(hour, min);
            KLog.i("onChargingGunStateChanged " + String.format("%2d:%2d", hour, min));
            mDataCache.put("ChargingRestTime", String.format("%2d:%2d", hour, min));
        }

        @Override
        public void onChargingGunStateChanged(int state) {
            super.onChargingGunStateChanged(state);
//            dataHolder.setChargeGunConnectState(state + "");
            KLog.i("onChargingGunStateChanged " + state);
            mDataCache.put("ChargingGunState", String.valueOf(state));
//            refreshUI();
        }

        @Override
        public void onChargerStateChanged(int state) {
            super.onChargerStateChanged(state);
            KLog.i("onChargerStateChanged " + state);
            mDataCache.put("ChargerState", String.valueOf(state));
//            dataHolder.setChargerConnectState(state + "");
//            refreshUI();
        }

        @Override
        public void onChargerWorkStateChanged(int state) {
            super.onChargerWorkStateChanged(state);
            KLog.i("onChargerWorkStateChanged " + state);
            mDataCache.put("ChargerWorkState", String.valueOf(state));
        }

        @Override
        public void onBatteryManagementDeviceStateChanged(int state) {
            super.onBatteryManagementDeviceStateChanged(state);
            KLog.i("onChargerWorkStateChanged " + state);
            mDataCache.put("BatteryManagementDeviceState", String.valueOf(state));

        }

        @Override
        public void onDataEventChanged(int eventType, BYDAutoEventValue eventValue) {
            super.onDataEventChanged(eventType, eventValue);
            KLog.i("onDataEventChanged 充电信息：" + eventType + " " + eventValue.intValue + " " + eventValue.floatValue + " " + eventValue.doubleValue);

//            refreshChargingInfo();

            ChargingDeviceHelper helper = ChargingDeviceHelper.getInstance(chargingDevice);

            if (eventType == BYDAutoFeatureIds.CHARGING_POWER) {
                mDataCache.put("ChargePower", "[" + eventValue.floatValue + "," + eventValue.doubleValue + "]");
            } else if (eventType == BYDAutoFeatureIds.CHARGING_CHARGE_POWER_DD) {
                mDataCache.put("ChargePower", "[" + eventValue.floatValue + "," + eventValue.doubleValue + "]");
            }

            if (eventType == BYDAutoFeatureIds.CHARGING_CHARGE_BATTERY_VOLT) {
                mDataCache.put("ChargeVolt", helper.getVoltage() + "");
            }
            if (eventType == BYDAutoFeatureIds.CHARGING_CHARGE_CURRENT) {
//                Object current = helper.getCurrent();
//                KLog.i();
//                int intValue = eventValue.intValue;
//                float floatValue = eventValue.floatValue;
//                double doubleValue = eventValue.doubleValue;
//                KLog.i("充电电流：" + intValue + " " + floatValue + " " + doubleValue);
                mDataCache.put("ChargeCurrent", String.valueOf(Math.abs(eventValue.doubleValue)));
            }
            if (eventType == BYDAutoFeatureIds.CHARGING_FULL_REST_MINUTE) {
                mDataCache.put("ChargeRestMinute", helper.getMinute() + "");
            }
            if (eventType == BYDAutoFeatureIds.CHARGING_FULL_REST_HOUR) {
                mDataCache.put("ChargeRestHour", helper.getHour() + "");
            }
            if (eventType == BYDAutoFeatureIds.CHARGING_BATTERRY_DEVICE_STATE) {
                mDataCache.put("BatteryDeviceState", helper.getBatteryDeviceState());
            }
            if (eventType == BYDAutoFeatureIds.CHARGING_GUN_CONNECT_STATE) {
                mDataCache.put("ChargeGunConnectState", helper.getGunConnect());
            }
            if (eventType == BYDAutoFeatureIds.CHARGING_CHARGER_CONNECT_STATE) {
                mDataCache.put("ChargerConnectState", helper.getChargerConnect());
            }
        }
    };

    private final AbsBYDAutoAcListener absBYDAutoAcListener = new AbsBYDAutoAcListener() {
        /**
         * 监听风量档位的变化
         * @param level
         */
        @Override
        public void onAcWindLevelChanged(int level) {
            super.onAcWindLevelChanged(level);
            mDataCache.put("CurrentWindLevel", String.valueOf(level));
        }

        /**
         * 监听各区域温度的变化
         * @param area
         * @param value
         */
        @Override
        public void onTemperatureChanged(int area, int value) {
            super.onTemperatureChanged(area, value);
            if (area == BYDAutoAcDevice.AC_TEMPERATURE_MAIN) {
                mDataCache.put("CurrentTemperature", String.valueOf(value));
            }
        }

        /**
         * 手动/自动
         * @param mode
         */
        @Override
        public void onAcCtrlModeChanged(int mode) {
            super.onAcCtrlModeChanged(mode);
            String modeStr = "unknown";
            if (mode == BYDAutoAcDevice.AC_CTRLMODE_AUTO) {
                modeStr = "auto";
            } else if (mode == BYDAutoAcDevice.AC_CTRLMODE_MANUAL) {
                modeStr = "manual";
            }
            mDataCache.put("AcCtrlMode", String.valueOf(modeStr));
        }

        @Override
        public void onAcStarted() {
            super.onAcStarted();

            mDataCache.put("AcStarted", Instant.now().toEpochMilli() + "");
        }

        @Override
        public void onAcStoped() {
            super.onAcStoped();
            mDataCache.put("AcStoped", Instant.now().toEpochMilli() + "");
        }

        /**
         * 内/外循环
         * @param mode
         */
        @Override
        public void onAcCycleModeChanged(int mode) {
            super.onAcCycleModeChanged(mode);
            String modeStr = "unknown";
            if (mode == BYDAutoAcDevice.AC_CYCLEMODE_OUTLOOP) {
                modeStr = "outloop";
            } else if (mode == BYDAutoAcDevice.AC_CYCLEMODE_INLOOP) {
                modeStr = "inloop";
            }

            mDataCache.put("AcCycleMode", modeStr);
        }

        /**
         * 压缩机状态改变
         * @param mode
         */
        @Override
        public void onAcCompressorModeChanged(int mode) {
            super.onAcCompressorModeChanged(mode);
            String modeStr = "unknown";
            if (mode == BYDAutoAcDevice.AC_COMPRESSOR_OFF) {
                modeStr = "off";
            } else if (mode == BYDAutoAcDevice.AC_COMPRESSOR_ON) {
                modeStr = "on";
            }

            mDataCache.put("AcCompressorMode", modeStr);
        }

        /**
         * 除霜
         * @param area
         * @param state
         */
        @Override
        public void onAcDefrostStateChanged(int area, int state) {
            super.onAcDefrostStateChanged(area, state);
//            if (area == BYDAutoAcDevice.AC_DEFROST_AREA_FRONT) {
//                if (defrostModeStatusTv != null) {
//                    if (state == BYDAutoAcDevice.AC_DEFROST_STATE_OFF) {
//                        defrostModeStatusTv.setSelected(false);
//                    } else if (state == BYDAutoAcDevice.AC_DEFROST_STATE_ON) {
//                        defrostModeStatusTv.setSelected(true);
//                    }
//                }
//            }
            mDataCache.put("AcDefrostState", String.format("%d: %d", area, state));
        }

        /**
         * 通风
         * @param state
         */
        @Override
        public void onAcVentilationStateChanged(int state) {
            super.onAcVentilationStateChanged(state);
//            if (ventilateStatusTv != null) {
//                if (state == BYDAutoAcDevice.AC_VENTILATION_STATE_OFF) {
//                    ventilateStatusTv.setSelected(false);
//                } else if (state == BYDAutoAcDevice.AC_VENTILATION_STATE_ON) {
//                    ventilateStatusTv.setSelected(true);
//                }
//            }
            mDataCache.put("AcVentilationState", String.format("%d:", state));

        }
    };

    private final AbsBYDAutoEngineListener absBYDAutoEngineListener = new AbsBYDAutoEngineListener() {
        @Override
        public void onEngineSpeedChanged(int value) {
            super.onEngineSpeedChanged(value);
            AutoStatisticDeviceHelper helper = AutoStatisticDeviceHelper.getInstance(statisticDevice);
            int waterTemperature = helper.getWaterTemperature();
            mDataCache.put("WaterTemperature", String.format("%d:", value));
        }

        @Override
        public void onEngineCoolantLevelChanged(int state) {
            super.onEngineCoolantLevelChanged(state);
            mDataCache.put("EngineCoolantLevel", String.format("%d:", state));
        }

        @Override
        public void onOilLevelChanged(int value) {
            super.onOilLevelChanged(value);
            mDataCache.put("OilLevel", String.format("%d:", value));
        }

        @Override
        public void onError(int errCode, String errMessage) {
            super.onError(errCode, errMessage);
            KLog.i();
        }

        @Override
        public void onDataEventChanged(int eventType, BYDAutoEventValue eventValue) {
            super.onDataEventChanged(eventType, eventValue);
            KLog.i("onDataEventChanged 引擎信息：" + eventType + " " + eventValue.intValue + " " + eventValue.floatValue + " " + eventValue.doubleValue);
            updateEngineSpeedData();
        }
    };

    /**
     * onEnergyModeChanged()
     * onSpeedChanged()
     * onAccelerateDeepnessChanged()
     * onPowerGenerationValueChanged()
     * initAutoData()
     */
    private void updateEngineSpeedData() {
        if (engineDevice == null) {
            return;
        }
        int engine_speed = BydApi29Helper.get(engineDevice, BYDAutoFeatureIds.ENGINE_SPEED);
        int engine_speed_gb = BydApi29Helper.get(engineDevice, BYDAutoFeatureIds.ENGINE_SPEED_GB);
        int engine_speed_warning = BydApi29Helper.get(engineDevice, BYDAutoFeatureIds.ENGINE_SPEED_WARNING);
        int engine_speed_result;
        if (engine_speed_gb > 0 && engine_speed_gb <= 8000) {
            engine_speed_result = engine_speed_gb;
        } else if (engine_speed > 0 && engine_speed <= 8000) {
            engine_speed_result = engine_speed;
        } else {
            engine_speed_result = 0;
        }
        mDataCache.put("EngineSpeed", engine_speed_result + "");
//        if (engineSpeedEsv != null) {
//            engineSpeedEsv.setVelocity(engine_speed_result);
//        }

        int front_motor_speed = BydApi29Helper.get(engineDevice, BYDAutoFeatureIds.ENGINE_FRONT_MOTOR_SPEED);
        int front_motor_torque = BydApi29Helper.get(engineDevice, BYDAutoFeatureIds.ENGINE_FRONT_MOTOR_TORQUE);

        int rear_motor_speed = BydApi29Helper.get(engineDevice, BYDAutoFeatureIds.ENGINE_REAR_MOTOR_SPEED);
        int rear_motor_torque = BydApi29Helper.get(engineDevice, BYDAutoFeatureIds.ENGINE_REAR_MOTOR_TORQUE);

        if (engine_speed_gb == 1) {
            engine_speed_gb = 0;
        }
        if (engine_speed_gb >= 8000) {
            engine_speed_gb = 0;
        }
//        dataHolder.setEngineSpeedGb(engine_speed_gb + "");
        mDataCache.put("EngineSpeedGb", engine_speed_gb + "");
        if (engine_speed_warning == 1) {
            engine_speed_warning = 0;
        }
        if (engine_speed_warning >= 8000) {
            engine_speed_warning = 0;
        }
//        dataHolder.setEngineSpeedWarning(engine_speed_warning + "");
        mDataCache.put("EngineSpeedWarning", engine_speed_warning + "");
        if (front_motor_speed == 1) {
            front_motor_speed = 0;
        }
        if (front_motor_speed >= 8000) {
            front_motor_speed = 0;
        }
        mDataCache.put("FrontMotorSpeed", Math.abs(front_motor_speed) + "");
        mDataCache.put("FrontMotorTorque", getValidTorqueValue(front_motor_torque) + "");
        if (rear_motor_speed == 1) {
            rear_motor_speed = 0;
        }
        if (rear_motor_speed >= 8000) {
            rear_motor_speed = 0;
        }
        mDataCache.put("RearMotorSpeed", Math.abs(rear_motor_speed) + "");
        mDataCache.put("RearMotorTorque", getValidTorqueValue(rear_motor_torque) + "");
    }

    private int getValidTorqueValue(int motor_torque) {
        if (motor_torque <= 0) {
            return 0;
        }
        if (motor_torque >= 65535) {
            return 0;
        }
        return motor_torque;
    }

    private final AbsBYDAutoEnergyListener absBYDAutoEnergyListener = new AbsBYDAutoEnergyListener() {
        /**
         * 监听能耗模式（EV/强制EV/HEV）
         * @param energyMode
         */
        @Override
        public void onEnergyModeChanged(int energyMode) {
            super.onEnergyModeChanged(energyMode);
//            dataHolder.setEnergyMode(StringUtils.getEnergyModeName(energyMode));
            mDataCache.put("EnergyMode", StringUtils.getEnergyModeName(energyMode));

            updateEngineSpeedData();

//            if (engineSpeedEsv != null) {
//                if (energyMode == BYDAutoEnergyDevice.ENERGY_MODE_EV || energyMode == BYDAutoEnergyDevice.ENERGY_MODE_FORCE_EV) {
//                    engineSpeedEsv.setVisibility(View.GONE);
//                } else {
//                    engineSpeedEsv.setVisibility(View.VISIBLE);
//                }
//            }
        }

        /**
         * 监听整车运行模式（经济模式，运动模式）
         * @param operationMode
         */
        @Override
        public void onOperationModeChanged(int operationMode) {
            super.onOperationModeChanged(operationMode);
            mDataCache.put("EnergyMode", StringUtils.getEnergyModeName(operationMode));
        }

        /**
         * 原地踩油门发电功率
         *
         * @param value
         */
        @Override
        public void onPowerGenerationValueChanged(int value) {
            super.onPowerGenerationValueChanged(value);
            mDataCache.put("PowerGenerationValue", value + "");
            updateEnginePower();
            updateEngineSpeedData();
        }
    };

    void updateEnginePower() {
        int enginePower = engineDevice.getEnginePower();
        mDataCache.put("EnginePower", enginePower + "");
    }



    private final AbsBYDAutoStatisticListener absBYDAutoStatisticListener = new AbsBYDAutoStatisticListener() {

        /**
         * 监听总里程变化
         * @param totalMileageValue
         */
        @Override
        public void onTotalMileageValueChanged(int totalMileageValue) {
            super.onTotalMileageValueChanged(totalMileageValue);
            //总里程
            mDataCache.put("TotalMileage", totalMileageValue + "");
            AutoStatisticDeviceHelper statisticDeviceHelper = AutoStatisticDeviceHelper.getInstance(statisticDevice);
            //里程1
//            dataHolder.setCustomMileage1(format.format(statisticDeviceHelper.getMileageNumber(0)));
            mDataCache.put("CustomMileage1", statisticDeviceHelper.getMileageNumber(0) + "");

            //里程2
//            dataHolder.setCustomMileage2(format.format(statisticDeviceHelper.getMileageNumber(1)));
            mDataCache.put("CustomMileage2", statisticDeviceHelper.getMileageNumber(1) + "");
            //总HEV里程
            int hevMileageValue = statisticDeviceHelper.getHEVMileageValue();
            mDataCache.put("TotalHevMileage", hevMileageValue + "");
//            dataHolder.setTotalHevMileage(hevMileageValue + "");
//            refreshUI();
            //总EV里程
            int evMileageValue = statisticDevice.getEVMileageValue();
            mDataCache.put("TotalEvMileage", evMileageValue + "");
            //更新单次行程能耗数据
            double totalElecConValue = statisticDevice.getTotalElecConValue();
            double totalFuelConValue = statisticDevice.getTotalFuelConValue();
            mDataCache.put("TotalElecCon", totalElecConValue + "");
            mDataCache.put("TotalFuelCon", totalFuelConValue + "");
//
//            //本次行程总里程
//            int total_mileage = totalMileageValue - init_totalMileageValue;
//            //本次行程ev里程
//            int ev_mileage = evMileageValue - init_evMileageValue;
//            //本次行程hev里程
//            int hev_mileage = hevMileageValue - init_hevMileageValue;
////            dataHolder.setCurrentTravelMileage(total_mileage + "");
//            mDataCache.put("CurrentTravelMileage", total_mileage + "");
//            //本次行程电耗
//            double elec_cost = totalElecConValue - init_totalElecConValue;
//            //同步车机负一屏计算方式，油发电不计算费用
//            if (elec_cost <= 0) {
//                elec_cost = 0;
//            }
//            //本次行程油耗
//            double fuel_cost = totalFuelConValue - init_totalFuelConValue;
//            //本次行程能耗(电耗+油耗)
//            String cost = format.format(elec_cost) + "度+" + format.format(fuel_cost) + "升";
//            dataHolder.setCurrentTravelEnergyCost(cost);
//            //本次行程花费
//            double yuan = elec_cost * init_latest_electric_price + fuel_cost * init_latest_fuel_price;
//            dataHolder.setCurrentTravelYuanCost(format.format(yuan));
//            //本次行程平均电耗(kwh/100km)
//            if (ev_mileage != 0) {
//                dataHolder.setCurrentTravelElecCost(format.format(elec_cost * 100 / ev_mileage));
//            } else {
//                dataHolder.setCurrentTravelElecCost(0 + "");
//            }
//
//            //本次行程平均油耗
//            if (hev_mileage != 0) {
//                dataHolder.setCurrentTravelFuelCost(format.format(fuel_cost * 100 / hev_mileage));
//            } else {
//                dataHolder.setCurrentTravelFuelCost("0");
//            }
////            }
//            //本次行程花费的钱相当于多少电
//            double valueKwh = yuan / init_latest_electric_price;//多少kwh
//            //本次行程花费的钱相当于多少油
//            double valueL = yuan / init_latest_fuel_price;//多少L
//            double c = 0;
//            double d = 0;
//            if (total_mileage != 0) {
//                //本次行程综合电耗
//                d = valueKwh * 100.0f / total_mileage;//kwh/100km
//                //本次行程综合油耗
//                c = valueL * 100.0f / total_mileage;//L/100km
//            }
//            dataHolder.setCurrentComprehensiveElecCost(format.format(d));
//            dataHolder.setCurrentComprehensiveFuelCost(format.format(c));
//            refreshUI();

        }

        /**
         * 监听燃油消耗总量变化
         * @param value
         */
        @Override
        public void onTotalFuelConChanged(double value) {
            super.onTotalFuelConChanged(value);
            mDataCache.put("TotalFuelCost", value + "");
        }

        /**
         * 监听电消耗总量的变化
         * @param value
         */
        @Override
        public void onTotalElecConChanged(double value) {
            super.onTotalElecConChanged(value);
            mDataCache.put("TotalElecCon", value + "");
        }

        /**
         * 监听最近百公里油耗变化
         * @param value {0,51.1}L/100KM
         */
        @Override
        public void onLastFuelConPHMChanged(double value) {
            super.onLastFuelConPHMChanged(value);
            mDataCache.put("LastFuelConPhm", value + "");
        }

        /**
         * 监听累计平均油耗变化
         * @param value
         */
        @Override
        public void onTotalFuelConPHMChanged(double value) {
            super.onTotalFuelConPHMChanged(value);
            mDataCache.put("TotalFuelConPhm", value + "");
        }


        /**
         * 监听最近百公里电耗变化
         * @param value {-99.9,99.9}KWH/100KM
         */
        @Override
        public void onLastElecConPHMChanged(double value) {
            super.onLastElecConPHMChanged(value);
            mDataCache.put("LastElecConPhm", value + "");
        }

        /**
         * 监听累计平均电耗变化
         * @param value
         */
        @Override
        public void onTotalElecConPHMChanged(double value) {
            super.onTotalElecConPHMChanged(value);
            mDataCache.put("TotalElecConPhm", value + "");
        }

        /**
         * 监听电续航里程变化
         * @param value
         */
        @Override
        public void onElecDrivingRangeChanged(int value) {
            super.onElecDrivingRangeChanged(value);
            mDataCache.put("ElecDrivingRange", value + "");

            AutoStatisticDeviceHelper statisticDeviceHelper = AutoStatisticDeviceHelper.getInstance(statisticDevice);
            mDataCache.put("LowestBatterVoltage", statisticDeviceHelper.getSTATISTIC_LOWEST_BATTERY_VOLTAGE() * 1.0f / 1000 + "");
            mDataCache.put("HighestBatterVoltage", statisticDeviceHelper.getSTATISTIC_HIGHEST_BATTERY_VOLTAGE() * 1.0f / 1000 + "");
            mDataCache.put("LowestBatterTemp", statisticDeviceHelper.getLOWEST_BATTERY_TEMP() + "");
            mDataCache.put("HighestBatterTemp", statisticDeviceHelper.getHIGHEST_BATTERY_TEMP() + "");
            mDataCache.put("AverageBatterTemp", statisticDeviceHelper.getAVERAGE_BATTERY_TEMP() + "");
        }

        /**
         * 监听燃油续航里程变化
         * @param value
         */
        @Override
        public void onFuelDrivingRangeChanged(int value) {
            super.onFuelDrivingRangeChanged(value);
//            if (value >= 2046) {
//                dataHolder.setFuelMileage("--");
//            } else {
//                dataHolder.setFuelMileage(value + "");
//            }
//            refreshUI();
            mDataCache.put("FuelDrivingRange", value + "");
        }

        /**
         * 监听燃油百分比变化
         * @param value
         */
        @Override
        public void onFuelPercentageChanged(int value) {
            super.onFuelPercentageChanged(value);
//            dataHolder.setFuelPb(value + "");
//            dataHolder.setFuelPercent(value);
//            refreshUI();
            mDataCache.put("FuelPercentage", value + "");
        }

        /**
         * 监听电量百分比变化
         * @param value
         */
        @Override
        public void onElecPercentageChanged(double value) {
            super.onElecPercentageChanged(value);
            if (value <= 1) {
                value = value * 100;
            }
//            dataHolder.setElecPb((int) ret + "");
//            dataHolder.setElecPercent(((int) ret));
//            refreshUI();
            mDataCache.put("ElecPercentage", value + "");
        }

        @Override
        public void onEVMileageValueChanged(int value) {
            super.onEVMileageValueChanged(value);
            mDataCache.put("EVMileageValue", value + "");
        }

        @Keep
        @Override
        public void onHEVMileageValueChanged(int value) {
            KLog.i("onHEVMileageValueChanged = " + value);
            mDataCache.put("TotalHevMileage", value + "");
        }

        /**
         * 瞬时电耗
         *
         * @param value
         */
        @Keep
        @Override
        public void onInstantElecConChanged(double value) {
//            if (value >= 0 && value < 100) {
//                dataHolder.setInstantElecCon(format.format(value));
//                refreshUI();
//            } else {
//                dataHolder.setInstantElecCon(format.format(0));
//                refreshUI();
//            }
            mDataCache.put("InstantElecCon", value + "");
        }

        /**
         * 瞬时油耗
         *
         * @param value
         */
        @Keep
        @Override
        public void onInstantFuelConChanged(double value) {
//            dataHolder.setInstantFuelCon(format.format(value));
//            refreshUI();
            mDataCache.put("InstantFuelCon", value + "");
        }
    };

    private void updateFrontMotorSpeed() {
        int front_motor_speed = BydApi29Helper.get(engineDevice, BYDAutoFeatureIds.ENGINE_FRONT_MOTOR_SPEED);
        if (front_motor_speed == 1) {
            front_motor_speed = 0;
        }
        mDataCache.put("FrontMotorSpeed", Math.abs(front_motor_speed) + "");
        int rear_motor_speed = BydApi29Helper.get(engineDevice, BYDAutoFeatureIds.ENGINE_REAR_MOTOR_SPEED);
        mDataCache.put("RearMotorSpeed", Math.abs(rear_motor_speed) + "");
    }


    private final AbsBYDAutoSpeedListener absBYDAutoSpeedListener = new AbsBYDAutoSpeedListener() {
        double currentSpeed;

        /**
         * 监听车速变化[0-282]km/h
         * @param currentSpeed
         */
        @Override
        public void onSpeedChanged(double currentSpeed) {
            super.onSpeedChanged(currentSpeed);
//            this.currentSpeed = currentSpeed;
//            dataHolder.setCarSpeed(format.format(currentSpeed));
            mDataCache.put("Speed", currentSpeed + "");

            updateEngineSpeedData();
            updateEnginePower();
            updateFrontMotorSpeed();
        }

        /**
         * 监听油门深度变化[0-100]%
         * @param value
         */
        @Override
        public void onAccelerateDeepnessChanged(int value) {
            super.onAccelerateDeepnessChanged(value);
            mDataCache.put("AccelerateDeepness", value + "");

            updateEngineSpeedData();

//            if (youMengPb != null) {
//                youMengPb.setProgress(value);
//            }
//            dataHolder.setYouMeng(value + "");
//            refreshUI();
        }

        /**
         * 监听制动深度变化[0-100]%
         * @param value
         */
        @Override
        public void onBrakeDeepnessChanged(int value) {
            super.onBrakeDeepnessChanged(value);
            mDataCache.put("BrakeDeepness", value + "");
//            if (shaChePb != null) {
//                shaChePb.setProgress(value);
//            }
//            dataHolder.setShaChe(value + "");
//            refreshUI();
        }

    };


    private final AbsBYDAutoInstrumentListener instrumentListener = new AbsBYDAutoInstrumentListener() {

        @Override
        public void onExternalChargingPowerChanged(double value) {
            super.onExternalChargingPowerChanged(value);
            mDataCache.put("ExternalChargingPower", value + "");
        }

        @Override
        public void onWheelTemperatureChanged(int position, int value) {
            super.onWheelTemperatureChanged(position, value);
            KLog.i("轮胎温度:" + position + " " + value);
            mDataCache.put("WheelTemperature", position + "," + value);
        }

        @Override
        public void onDataEventChanged(int eventType, BYDAutoEventValue eventValue) {
            super.onDataEventChanged(eventType, eventValue);
            String builder = eventValue.intValue + "," + eventValue.floatValue + "," + eventValue.doubleValue + "," + Arrays.toString(eventValue.intArrayValue) + "," + Arrays.toString(eventValue.floatArrayValue) + "," + Arrays.toString(eventValue.bufferDataValue);
            KLog.i("onDataEventChanged 仪表信息" + eventType + " ,data = [" + builder + "]");

            //充电完成回调
            //public static final int INSTRUMENT_EXTERNAL_CHARGING_POWER = 1246789648;//0x4a508010
            //2023-06-02 12:58:03.596 30919-30919 BYDAutoInstrumentDevice com.byd.negativescreen               D  postEvent device_type: 1007, event_type =4a508010, value = 2327.8
        }
    };


}
