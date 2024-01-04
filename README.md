# Static_IP_For_Your_FTP_Server
Implementation to get static ip at 192.168.49.1 to host a ftp server locally within your android device
# The Problem
It is no more possible to get a static IP for your ftp connection file transfer if you are using the personal hotspot technology unless you root the device and flash it with a custom ROM (where you set the ip manually in android's AOSP). This is unscalable and isnt a permanent solution.

# The Solution
Switching to WifiDirect Hotspot technology gives a static ip as it is still hard coded in the android AOSP. Here's an implementation for this. The ip will be 192.168.49.1. This implementation also gives user flexibilty to choose the speed of file transfer by setting the frequency at 2.4GHz or 5GHz. The user can also choose to set the SSID and password right from the app's interface, which is not possible with traditional hotspot, as restricted by android's security protocols.

# Maintainence

The project is maintained by Debojeet Bhattacharjee
debojeetbh123@gmail.com

