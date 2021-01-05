# android-app-qr-example

## What it does

This project demonstrates an implementation of the Simultaneous Localization and Mapping (SLAM) functionality of the MotionDNA SDK core.

The projects messages read from detected QR codes as an identifier in a [SLAM observation](https://github.com/navisens/NaviDocs/blob/master/API.Android.md#simultaneous-localization-and-mapping-slam).

## Setup

In our SDK we provide `MotionDnaSDK` class and `MotionDnaSDKListener` interface. In order for MotionDna to work, we need a class implements all callback methods in the interface.

```java
public class MainActivity extends AppCompatActivity implements MotionDnaSDKListener {
    @Override
    public void receiveMotionDna(MotionDna motionDna) {...}
    @Override
    public void reportStatus(MotionDnaSDK.Status status, String s) {...}
}
```

Enter your developer key in `startMotionDna()` in `MainActivity.java` and run the app. If you do not have one yet then please navigate to our [developer sign up page](https://www.navisens.com/index.html#contact) to request a free key.

```java
public void startMotionDna() {
    String devKey = "<--DEVELOPER-KEY-HERE-->";
    ...
}
```

## Get your Cartesian coordinates

In the `receiveMotionDna()` callback method we return a `MotionDna` estimation object which contains [location, heading and motion type](https://github.com/navisens/NaviDocs/blob/master/API.Android.md#estimation-properties) among many other interesting data on a users current state. Here is how we might print the cartesian values out.

```java
@Override
public void receiveMotionDna(MotionDna motionDna)
{
    statisticsTextView.setText(String.format(Locale.US,
        "x: %f \n" +
        "y: %f \n" +
        "z: %f",
        motionDna.getLocation().cartesian.x,
        motionDna.getLocation().cartesian.y,
        motionDna.getLocation().cartesian.z));
}
```

## Record a SLAM observation (from a QR code)

The project scans a QR code using `BarcodeCaptureActivity` activity and receives a String message from the QR code as its result.

The activity is started using

```java
Intent intent = new Intent(this, BarcodeCaptureActivity.class);
startActivityForResult(intent, RC_BARCODE_CAPTURE);
```

The result is received using `onActivityResult`. The hash code of the String message received from the QR code is used as an identifier and recorded as a new [SLAM observation](https://github.com/navisens/NaviDocs/blob/master/API.Android.md#simultaneous-localization-and-mapping-slam) with an uncertainty of `1.0` meter.

```java
@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (data != null) {
        Barcode barcode = data.getParcelableExtra(BarcodeCaptureActivity.BarcodeObject);
        int identifier = barcode.hashCode();
        motionDnaSDK.recordObservation(identifier, 1.0);
    }
    ...
}
```

## Common task

A user is indoors and revisits the same areas frequently. Through some outside mechanism the developer is aware of a return to certain landmarks and would like to indicate that the user has returned to a landmark with ID of 38 to aid in the estimation of a user's position. The developer also knows that this observation was made within `3.0` meters of the landmark `38`.

```java
motionDnaSDK.recordObservation(38, 3.0);
```
