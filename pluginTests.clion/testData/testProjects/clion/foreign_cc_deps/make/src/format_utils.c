#include "format_utils.h"
#include <stdio.h>
#include <string.h>

int format_greeting(char *buffer, int buffer_size, const char *name) {
    if (!buffer || buffer_size <= 0 || !name) {
        return -1;
    }
    return snprintf(buffer, (size_t)buffer_size, "Hello, %s!", name);
}

int reverse_string(char *str) {
    if (!str) {
        return -1;
    }
    int len = (int)strlen(str);
    for (int i = 0, j = len - 1; i < j; ++i, --j) {
        char temp = str[i];
        str[i] = str[j];
        str[j] = temp;
    }
    return 0;
}
