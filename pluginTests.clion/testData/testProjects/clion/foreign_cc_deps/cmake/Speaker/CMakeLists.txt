add_library (speaker 
  Speaker.h
  Speaker.cpp)

set_property(TARGET speaker PROPERTY C_STANDARD 23)
install (TARGETS speaker DESTINATION lib)
install (FILES Speaker.h DESTINATION include/greeting/v1.23)
