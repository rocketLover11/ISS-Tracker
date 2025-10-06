if whichjava >/dev/null 2>&1; then
    java -jar isstracker-1.0.jar
else
    dialog --title "Java Needed" --msgbox "Java is needed to run ISS Tracker.\nDownload Java at https://www.java.com/en/downloads/" 7 40
fi