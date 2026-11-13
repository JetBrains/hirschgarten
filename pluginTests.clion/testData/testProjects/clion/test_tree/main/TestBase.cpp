#include <catch2/catch_test_macros.hpp>

SCENARIO( "tree test", "[tree]" ) {
   SECTION( "Common") {
      SECTION( "1, comma" ) {
         CHECK( 1 == 2 );
      }
      SECTION( "2" ) {
         CHECK( 2 == 2 );
      }
   }
}

TEST_CASE( "SUCCEED counts as a test pass", "[messages]" )
{
   SUCCEED("this is a success");
}

TEST_CASE( "Output from all sections is reported", "[failing][messages]" )
{
    SECTION( "one" )
    {
        FAIL( "Message from section one" );
    }

    SECTION( "two" )
    {
        FAIL( "Message from section two" );
    }
}

TEST_CASE( "checkedIf, failing", "[failing][comma]" )
{
    REQUIRE( false );
}

TEST_CASE( "files" )
{
   SUCCEED( "files" );
}

// The name holds a slash, but it is not a section of "files"
TEST_CASE( "files/read" )
{
   SECTION( "partial" )
   {
      CHECK( 1 == 2 );
   }
}
