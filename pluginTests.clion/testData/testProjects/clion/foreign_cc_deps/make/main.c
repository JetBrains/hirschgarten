#include <stdio.h>
#include "format_utils.h"

int main(void) {
    char greeting[64];
    format_greeting(greeting, sizeof(greeting), "Bazel User");

    char text[32] = "Make Library";
    printf("Make C Library Example:\n");
    printf("%s\n", greeting);

    printf("Original: %s\n", text);
    reverse_string(text);
    printf("Reversed: %s\n", text);

    return 0;
}
