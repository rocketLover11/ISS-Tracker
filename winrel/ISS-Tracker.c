#include <windows.h>
#include <stdlib.h>

int main() {
    int res = system("java -version >nul 2>&1");
    if (res != 0) {
        MessageBox(NULL, "Java is needed for this program.\nPlease Install Java at https://www.java.com/en/download/.", "Java Required", MB_ICONERROR | MB_OK);
        return 1;
    }

    res = system("java -jar isstracker-1.0.jar");
    if (res != 0) {
        MessageBox(NULL, "Failed to start the Java program.", "Error", MB_ICONERROR | MB_OK);
    }

    return 0;
}