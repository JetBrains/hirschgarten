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

class TestWithParam : public ::testing::TestWithParam<int> {
};
TEST_P(TestWithParam, Fails1) {
    EXPECT_FALSE(GetParam() == 1);
}
TEST_P(TestWithParam, Fails5) {
    EXPECT_FALSE(GetParam() == 5);
}
INSTANTIATE_TEST_SUITE_P(Instantiation1to3, TestWithParam, ::testing::Values(1, 2, 3));
INSTANTIATE_TEST_SUITE_P(Instantiation4to6, TestWithParam, ::testing::Values(4, 5, 6));
INSTANTIATE_TEST_SUITE_P(/*empty*/, TestWithParam, ::testing::Values(1, 5));

template <typename T>
class TypedTest : public ::testing::Test {
};
typedef ::testing::Types<char, int, unsigned int> MyTypes;
TYPED_TEST_SUITE(TypedTest, MyTypes);
TYPED_TEST(TypedTest, success) {
}
TYPED_TEST(TypedTest, failure) {
    EXPECT_EQ(4, sizeof(TypeParam));
}

template <typename T>
class TypeParameterizedTest : public ::testing::Test {
};
TYPED_TEST_SUITE_P(TypeParameterizedTest);
TYPED_TEST_P(TypeParameterizedTest, success) {
}
TYPED_TEST_P(TypeParameterizedTest, failure) {
    EXPECT_EQ(4, sizeof(TypeParam));
}
REGISTER_TYPED_TEST_SUITE_P(TypeParameterizedTest, success, failure);
INSTANTIATE_TYPED_TEST_SUITE_P(Instantiation, TypeParameterizedTest, MyTypes);
