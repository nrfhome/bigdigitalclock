# The Android Clock Project

This project lets you recycle obsolete Android devices into
self-syncing digital clocks.

Due to the expense involved in maintaining the massive software stack on
modern smartphones, phone and tablet hardware frequently outlives its
software updates by 5+ years.  Lacking urgent security fixes and
API updates, these devices are unsuitable for day-to-day use but can
sometimes be repurposed for specific applications.

While there are many other clock apps on the Play Store, this one is
unique because it allows the time to be set via Bluetooth Low Energy
broadcasts (see below) rather than over IP.

# Setup options

## Easy: NTP over IP

If you connect an Android device to the internet, it will automatically
set the system time using NTP.  You can then run this app, and by
default, it will display the Android system time on the screen, using
the timezone configured in Android Settings.

Since old Android OS images do not have up-to-date firmware for any of
the wireless interfaces, the ideal setup is:

 * Android device with USB-C.
 * USB-C "dock" that provides charging + wired ethernet.  One such device
is [here](https://www.amazon.com/gp/product/B09MTN8ZT3).
 * Wired ethernet connected to an isolated VLAN.
 * Restricted VLAN access: DHCP, NTP, captive portal detect only.
 * Airplane Mode; all on-device radios disabled.
 * If possible, disassemble the device to physically remove cameras and
microphones.

If you must use WiFi, you should enable Airplane Mode and then enable
WiFi.  This turns off the cellular radio.  If the cellular radio is left
on, the device may receive Emergency Alerts and other nuisance
transmissions.  It could also be a security hazard.

If you want to lock down the IP network segment, you can hardcode the
NTP server address for time.google.com:

    settings put global ntp_server 216.239.35.0

and then create a firewall rule on your router that only allows
outbound traffic to that IP, nothing else.

## More involved: timebeacon broadcasts received by Android

Since it isn't a good idea to connect unpatched devices to an IP
network, the next best option is to set up a [timebeacon
dongle](https://github.com/nrfhome/nrfhome/blob/master/timebeacon/README.md)
which continuously broadcasts the current time throughout your house
using Bluetooth Low Energy advertisement packets.  The Android device
would then scan for these broadcasts and use them to set the time.

In this setup:

 * The Android device stays in Airplane Mode, but with Bluetooth enabled.
 * No IP connectivity is enabled.
 * No external device needs to be connected to the Android (just a charger).
 * Timezone and DST adjustments are handled on the transmitter, not by
the app or by the Android OS.

The main disadvantage of this setup is that security researchers have
discovered zero-click attacks exploiting Android's Bluetooth stack, so
it's possible that many or most old Android OS builds are exploitable
over the air by an attacker within close range.  You'll want to check
whether your build is affected by:

 * CVE-2022-20345 affecting Android 11 and 12.
 * CVE-2020-0022 affecting Android 8, 9, and possibly others.
 * Any other Bluetooth/BLE remote code execution vulnerabilities.

To use this option, set up a timebeacon on a nearby Linux system and
then enable "Use Bluetooth" in the settings menu.

Currently the app will display "SYNC" while it's scanning for
BLE beacons.  If it successfully syncs, it will display the time.  If
it later loses sync, it will change the digits to red so that user
knows it has lost contact with the timebeacon device.

If the display is stuck on "SYNC 0000", that means it isn't seeing any
BLE advertisements at all (even non-timebeacon packets).  Check:

 * Is Bluetooth enabled?
 * Is Location enabled?  Many versions of Android require Location access
to receive BLE advertisements.
 * Go into Settings and make sure the Digital Clock app has all
permissions enabled.

If the display shows SYNC and a nonzero number, that means it is
receiving other BLE advertisements but it hasn't seen a timebeacon
advertisement.  Check the timebeacon setup (if you have a
[BLE sniffer](https://www.nordicsemi.com/Products/Development-tools/nrf-sniffer-for-bluetooth-le)
available, that is ideal).

## Most advanced: timebeacon broadcasts received by client dongles

TBA

# Basic usage

To enter the Settings menu, touch the screen while the clock is up.
Depending on your Android version, you may have to touch it a second time
after the system navigation bar appears.  When you see the white
settings icon, click on it.

# Other considerations

## Battery swelling

It is generally a bad idea to keep lithium ion batteries continuously
charged to 100% for extended periods of time.  This degrades their
capacity and may cause them to fill with gas (swell), physically
damaging the device and creating a hazard.

 * If your device natively supports limiting the charge to a number lower
than 100%, you can enable that feature in Settings.  On Samsung this is
called "Protect Battery."
 * Third-party ROMs may offer this feature if it is missing from the
manufacturer's ROM.  See discussion below.
 * If you can root the device, you can try the
[Battery Charge Limit](https://play.google.com/store/apps/details?id=com.slash.batterychargelimit) or
[Advanced Charging Controller (ACCA)](https://f-droid.org/en/packages/mattecarra.accapp/)
apps.
 * If all else fails, you can physically remove the battery and supply
~4 volts on the internal terminals.  This is not recommended, but it
might be the only way to salvage an otherwise-working phone with a
ruined battery.  (Most Android devices will not boot without a battery
connected internally.)  Your supply might have to source up to 5A to deal
with the SoC's highest performance states.

## Power failure behavior

By default, this app will keep the screen on as long as the device has
AC power (i.e. power supplied through the charging port).  If it detects
a transition to battery power, it will allow the OS to power off the
display.  When AC power is restored, the OS will probably wake up the
screen again.  With the screen off, the device will be able to survive
long power outages with no user intervention.

This behavior can be changed in Settings.

You may want to set the Android screen timeout for a couple of minutes,
so that it stays on during brief power interruptions.

## Screen burn-in

OLED screens are vulnerable to burn-in, especially if the brightness is
set too high.  This may render the device unsuitable for other purposes
after it has been used as a digital clock for weeks/months.

This may be partially mitigated by the "Move Clock" option in the
Settings menu.  Lowering the screen brightness can also help.

## Third-party Android ROMs

This step is optional but recommended.  The clock app will usually work fine with the last official ROM update from the device manufacturer.  Installing a newer OS build offers the following benefits:

 * If there are known security bugs in the Android Bluetooth stack, a newer OS build will (hopefully!) correct them.
 * Rooting the device allows you to run Battery Charge Limit, to prevent the battery from overcharging and swelling.

Here are the steps I followed to set up each of my old Nexus devices.  The procedure may vary somewhat, but this will give you a general idea of what to expect:

 * Unlock the bootloader using `fastboot flashing unlock`.  This erases all data on the device, which you probably want.
 * Find a recent custom ROM built for the device.
 	* Newer devices are officially supported by e.g. [LineageOS](https://lineageos.org/).  Or choose any other custom ROM that you prefer.
 	* For older devices, I searched for the device forum at XDA Developers ([example](https://www.google.com/search?q=nexus+5x+xda)), looked for the Android Development subforum, and browsed the threads marked `[ROM]` for recent unofficial builds.
 	* Download the zip file.  The filename may be something like `lineage-20.0-20230222-UNOFFICIAL-shamu.zip` and there should be a `META-INF` directory at the top level inside the zip.  You don't need to extract it yet.
 * Boot into TWRP Recovery:
    * Download the latest TWRP Recovery image for your device from <https://twrp.me/>
    * Reboot the device into fastboot.  The exact procedure varies from one device to the next, but usually involves holding down VolUp or VolDown while powering up.
    * Connect the device to the computer via USB.
    * On Linux systems, you may need to set permissions with [udev rules](https://github.com/M0Rf30/android-udev-rules/blob/main/51-android.rules) in order for fastboot to access the device.
 * Flash the custom ROM using TWRP:
 	* Run `fastboot boot twrp*img`
 	* In TWRP, "swipe to allow modifications"
 	* Wipe -> Format data -> type `yes`
    * Back, Back
    * Advanced Wipe -> select all checkboxes -> swipe
    * Back, Back, Back
    * Advanced -> ADB Sideload -> swipe to start
    * On the PC side, send the OS image down to the phone.  e.g. `adb sideload lineage-20.0-20230222-UNOFFICIAL-shamu.zip`.  Probably takes a while on old phones.
    * When this finishes, Reboot System back into Android
 * Proceed through the setup wizard.  Skip whatever steps you can.  Leave Location enabled.
 * Settings -> Security -> Screen lock -> None (optional)
 * [Enable USB debugging](https://developer.android.com/tools/adb) on the newly installed image.
    * When the confirmation prompt appears, tell Android to "always trust" your PC.
 * Install the necessary APK files using `adb install`:
	 * [Magisk](https://github.com/topjohnwu/MagiskManager/releases) to enable root apps
	 * [Battery Charge Limit](https://f-droid.org/en/packages/com.slash.batterychargelimit/) which requires root
	 * [BigDigitalClock](https://github.com/nrfhome/bigdigitalclock/releases)
* Set up Magisk and Battery Charge Limit:
   * Extract `boot.img` from the OS image zip file mentioned above.  Copy it to the device with `adb push boot.img /sdcard/Download`
      * Start the Magisk app and follow the instructions [here](https://topjohnwu.github.io/Magisk/install.html) to patch `boot.img` and flash it to the boot partition.
   * When the device reboots, run Battery Charge Limit.  If all goes well, it will obtain root access and it will be able to override the charging logic.
   * I set my charging limits to 55-45, based on the assumption that Li-Ion batteries should be stored around 50% charged.  The exact limit might not matter too much; what's important is to avoid keeping them topped off over 80% for prolonged periods of time.
 * Enable Airplane Mode and then manually enable Bluetooth.
 * Mute the system volume.
 * Start the Digital Clock app and configure it to your liking.
 * Hang it on the wall and attach power.

Some ROMs will natively support battery charge limiting, so if that is the case you can skip the Magisk and Battery Charge Limit steps.  Unfortunately I was not able to find such an image for my older hardware.

# Attribution

The original version of this clock app can be found at
<https://github.com/andreas1724/bigdigitalclock>
