#ifndef FORMAT_UTILS_H
#define FORMAT_UTILS_H

#ifdef __cplusplus
extern "C" {
#endif

int format_greeting(char *buffer, int buffer_size, const char *name);
int reverse_string(char *str);

#ifdef __cplusplus
}
#endif

#endif // FORMAT_UTILS_H
