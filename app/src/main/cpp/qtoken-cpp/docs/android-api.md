# VIN Android API

- The `cpp` and `Java` folders define a simple API for using the VIN in Android applications/plugins.
- To get started, copy both folders to `<PROJ>/app/src/main/`. The Java code is prepackaged in the Virgil namespace and must be packaged separately to other Java namespaces.
- In the class that represents the lifetime of your application, store a `VINBridgeCPP` object.
- Create a `startVIN()` function that is able to instantiate a VIN node as follows (this example uses `SharedPreferences`):

``` java
public static SharedPreferences vinSharedPref;

public static void startVIN() {
        // -- Initialization of VIN ----------------------------------- //

        // Shared Preferences
        vinSharedPref = mContext
                .getSharedPreferences("VIN_Shared_Pref", MODE_PRIVATE);

        String bootstrapIP = vinSharedPref.getString("bootstrap_ip", "");
        if(bootstrapIP == null || bootstrapIP.equals("")) {
            Toast.makeText(mContext,
                    "Please set the Bootstrap IP",
                                Toast.LENGTH_LONG).show();
            return;
        }

        VIN = new VINBridgeCPP();

        VINFolder vinFolder = new VINFolder(mContext);
        sleep(500); // intended to give enough time to instantiate - will be phased away

        new Thread( new Runnable() {​​​​​​​ @Override public void run() {
            while(VINFolder.rootFolder.equals("")) {
                vinFolder.isStoragePermissionGranted();
                sleep(500); // intended to give enough time to instantiate - will be phased away
            }

            Random r = new Random();
            vinNodePort = String.valueOf(r.nextInt(9999) + 8002);
            vinReceiptPort = String.valueOf(r.nextInt(9999) + 8002);
            VIN.run(bootstrapIP, vinNodePort, vinReceiptPort, VINFolder.rootFolder);
        }​​​​​​​ }​​​​​​​ ).start();
        // ----------------------------------- Initialization of VIN -- //
    }
```


- At this point, you will be able to utilize the VIN using the standard api:
  - `VIN.put(String key, String message)`
  - `VIN.get(String key)`
  - `VIN.spread(String filePath)`
  - `VIN.gather()`
  - `VIN.share(String filePath, String receiverIP, String receiverReceiptPort)`

- NOTE: As of this release, `VIN.spread` writes the receipt to `${VIN}/receipts/sent/test/txt` and gather retrieves the VIN file from the same receipt path. This behavior will be fixed in the next release and use of `VIN.share` is recommended in the meanwhile.
- NOTE: Android performance is unusually slow compared to RasPi and x86. This is actively being investigated and will be remedied ASAP.
