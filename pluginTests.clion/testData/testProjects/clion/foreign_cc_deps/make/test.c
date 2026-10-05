#include <assert.h>
#include <stdio.h>
#include <string.h>
#include "format_utils.h"

int main(void) {
    char buf[64];
    int res = format_greeting(buf, sizeof(buf), "Alice");
    assert(res > 0);
    assert(strcmp(buf, "Hello, Alice!") == 0);

    char str[] = "hello";
    assert(reverse_string(str) == 0);
    assert(strcmp(str, "olleh") == 0);

    printf("All format_utils tests passed!\n");
    return 0;
}
