#include <gtest/gtest.h>

#include "TestInHeader.h"

TEST(Test1, InImplementation) {
    EXPECT_TRUE(true);
}

GTEST_TEST(Test2, gtest_test) {
    EXPECT_TRUE(true);
}

TEST(Test_Name, With_Underscores) {
    EXPECT_TRUE(true);
}

class TestFixture : public ::testing::Test {
};
TEST_F(TestFixture, success) {
    EXPECT_TRUE(true);
}
