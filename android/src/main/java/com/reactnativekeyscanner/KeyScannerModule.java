package com.reactnativekeyscanner;

import android.app.Activity;
import android.view.KeyEvent;
import android.view.inputmethod.InputMethodManager;
import androidx.annotation.NonNull;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@ReactModule(name = KeyScannerModule.NAME)
public class KeyScannerModule extends ReactContextBaseJavaModule {

  public static final String NAME = "KeyScanner";
  private ReactContext mReactContext;
  private DeviceEventManagerModule.RCTDeviceEventEmitter mJSModule = null;
  private static KeyScannerModule instance = null;
  private long last_up = 0;
  private String input_cache = "";
  private int submitKeycode = 66;

  public KeyScannerModule(ReactApplicationContext reactContext) {
    super(reactContext);
    instance = this;
    mReactContext = reactContext;
  }

  public static KeyScannerModule getInstance() {
    return instance;
  }

  @Override
  @NonNull
  public String getName() {
    return NAME;
  }

  @ReactMethod
  public void hideKeyboard() {
    final Activity activity = getCurrentActivity();
    if (activity != null) {
      InputMethodManager imm = (InputMethodManager) activity.getSystemService(
        Activity.INPUT_METHOD_SERVICE
      );
      if (imm != null) {
        imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0); // hide
      }
    }
  }

  @ReactMethod
  public void setSubmitKeycode(int keyCode) {
    this.submitKeycode = keyCode;
  }

  public void onKeyDownEvent(int keyCode, KeyEvent keyEvent) {
    if (mJSModule == null) {
      mJSModule =
        mReactContext.getJSModule(
          DeviceEventManagerModule.RCTDeviceEventEmitter.class
        );
    }
    mJSModule.emit("onKeyDown", getJsEventParams(keyCode, keyEvent, null));
  }

  public void onKeyUpEvent(int keyCode, KeyEvent keyEvent) {
    if (mJSModule == null) {
      mJSModule =
        mReactContext.getJSModule(
          DeviceEventManagerModule.RCTDeviceEventEmitter.class
        );
    }
    mJSModule.emit("onKeyUp", getJsEventParams(keyCode, keyEvent, null));
    if ((new Date().getTime() - last_up) > 1000) { // 1 seconds
      input_cache = "" + keyEvent.getDisplayLabel();
      last_up = 0;
    } else {
      // TODO: Check valid keycode only
      if (keyCode != submitKeycode) {
        input_cache += keyEvent.getDisplayLabel();
      }
    }
    if (input_cache.length() > 1 && keyCode == submitKeycode) {
      WritableMap params = new WritableNativeMap();
      params.putString("data", input_cache);
      mJSModule.emit("barcode_scan", params);
    }
    last_up = new Date().getTime();
  }

  public void onKeyMultipleEvent(
    int keyCode,
    int repeatCount,
    KeyEvent keyEvent
  ) {
    if (mJSModule == null) {
      mJSModule =
        mReactContext.getJSModule(
          DeviceEventManagerModule.RCTDeviceEventEmitter.class
        );
    }
    mJSModule.emit(
      "onKeyMultiple",
      getJsEventParams(keyCode, keyEvent, repeatCount)
    );
  }

  private WritableMap getJsEventParams(
    int keyCode,
    KeyEvent keyEvent,
    Integer repeatCount
  ) {
    WritableMap params = new WritableNativeMap();
    int action = keyEvent.getAction();

    if (
      keyEvent.getAction() == KeyEvent.ACTION_MULTIPLE &&
      keyCode == KeyEvent.KEYCODE_UNKNOWN
    ) {
      String chars = keyEvent.getCharacters();
      if (chars != null) {
        params.putString("characters", chars);
      }
    }

    if (repeatCount != null) {
      params.putInt("repeatcount", repeatCount);
    }

    params.putInt("keyCode", keyCode);
    params.putInt("action", action);
    params.putString("keyLabel", "" + keyEvent.getDisplayLabel());

    return params;
  }
  // Example method
  // See https://reactnative.dev/docs/native-modules-android
  // @ReactMethod
  // public void multiply(int a, int b, Promise promise) {
  //     promise.resolve(a * b);
  // }

  // public static native int nativeMultiply(int a, int b);
}
