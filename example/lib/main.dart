import 'dart:io';

import 'package:flutter/material.dart' hide Intent;
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:linker/linker.dart';

void main() => runApp(new MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => new _MyAppState();
}

class Button extends StatelessWidget {
  final String label;

  final VoidCallback onPressed;

  Button({required this.label, required this.onPressed});

  @override
  Widget build(BuildContext context) {
    return new RaisedButton(
        onPressed: onPressed,
        color: Colors.blueAccent,
        textColor: Colors.white,
        child: Text(label));
  }
}

class AndroidExample extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return new Center(
      child: new Column(
        mainAxisSize: MainAxisSize.min,
        children: <Widget>[
          new Button(
              onPressed: () async {
                try {
                  await Linker.openSetting();
                } on PlatformException catch (e) {
                  print("Open failed $e");
                }
              },
              label: "Open Settings"),
          new Button(
              onPressed: () async {
                try {
                  await Linker.startActivity(Intent.fromAction(Intent.ACTION_VIEW,
                      uri: Uri.parse("mqqwpa://im/chat?chat_type=wpa&uin=123456")));
                } on PlatformException catch (e) {
                  print("Open failed $e");
                }
              },
              label: "Open QQ"),
          new Button(
              onPressed: () async {
                try {
                  await Linker.startActivity(new Intent.callApp( packageName:  "com.tencent.mm",className: "com.tencent.mm.ui.LauncherUI"));
                } on PlatformException catch (e) {
                  print("Open failed $e");
                }
              },
              label: "Open Wechat"),
          new Button(
              onPressed: () async {
                try {
                  await Linker.openNetworkSetting();
                } on PlatformException catch (e) {
                  print("Open failed $e");
                }
              },
              label: "Open Network settings"),
          new Button(
              onPressed: () async {
                try {
                  await Linker.startActivity(Intent.fromAction(
                    "android.intent.action.VIEW",
                    uri: Uri.parse(
                        "http://ditu.google.cn/maps?hl=zh&mrt=loc&q=31.1198723,121.1099877(上海青浦大街100号)"),
                  ));
                } on PlatformException catch (e) {
                  print("Open failed $e");
                }
              },
              label: "Open google map"),
        ],
      ),
    );
  }
}

class IosExample extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return new Center(
      child: new Column(
        mainAxisSize: MainAxisSize.min,
        children: <Widget>[
          new Button(
              onPressed: () async {
                try {
                  await Linker.openURL("weixin://");
                } on PlatformException catch (e) {
                  print("Open failed $e");
                }
              },
              label: "Open Wechat"),
          new Button(
              onPressed: () async {
                try {
                  await Linker.openSetting();
                } on PlatformException catch (e) {
                  print("Open failed $e");
                }
              },
              label: "Open Settings"),
          new Button(
              onPressed: () async {
                try {
                  await Linker.openURL("tel:10086");
                } on PlatformException catch (e) {
                  print("Open failed $e");
                }
              },
              label: "Call 10086"),
        ],
      ),
    );
  }
}

class _MyAppState extends State<MyApp> {
  @override
  void initState() {
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return new MaterialApp(
      home: new Scaffold(
        appBar: new AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Platform.isAndroid ? new AndroidExample() : new IosExample(),
      ),
    );
  }
}
