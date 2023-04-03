# bigdigitalclock

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

 * Android device with USB-C
 * USB-C "dock" that provides charging + wired ethernet
 * Wired ethernet connected to an isolated VLAN
 * Restricted VLAN access: DHCP, NTP, captive portal detect only
 * Airplane Mode; all on-device radios disabled
 * If possible, open the device to disconnect cameras and microphones

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

 * The Android device stays in Airplane Mode, but with Bluetooth enabled
 * No IP connectivity is enabled
 * No external device needs to be connected to the Android (just a charger)
 * Timezone and DST adjustments are handled on the transmitter, not by
the app or by the Android OS

The main disadvantage of this setup is that security researchers have
discovered zero-click attacks exploiting Android's Bluetooth stack, so
it's possible that many or most old Android OS builds are exploitable
over the air by an attacker within close range.  You'll want to check
whether your build is affected by:

 * CVE-2022-20345 affecting Android 11 and 12
 * CVE-2020-0022 affecting Android 8, 9, and possibly others
 * Any other Bluetooth/BLE remote code execution vulnerabilities

To use this option, set up a timebeacon on a nearby Linux system and
then enable "Use Bluetooth" in the settings menu.

Currently the app will display a black screen while it's scanning for
BLE beacons.  If it successfully syncs, it will display the time.  If
it later loses sync, it will change the digits to red so that user
knows it has lost contact with the timebeacon device.

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
 * Third-party ROMs, such as LineageOS, may offer this feature if it is
missing from the manufacturer's ROM
 * If you can root the device, you can try the
[Battery Charge Limit](https://play.google.com/store/apps/details?id=com.slash.batterychargelimit) or
[Advanced Charging Controller (ACCA)](https://f-droid.org/en/packages/mattecarra.accapp/)
apps.
 * If all else fails, you can physically remove the battery and supply
~4 volts on the internal terminals.  This is not recommended, but it
might be the only way to salvage an otherwise-working phone with a
ruined battery.  (Most Android devices will not boot without a battery
connected internally.)

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
Settings menu.

# Attribution

The original version of this clock app can be found at
<https://github.com/andreas1724/bigdigitalclock>
