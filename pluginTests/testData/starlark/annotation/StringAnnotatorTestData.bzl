"Hello world!"
<error descr="Missing closing quote [\"]">"Hello world!</error>
<error descr="Missing closing quote [\"]">"Hello world!\"</error>
"This literal contains even number of backslashes\\\\\\"
<error descr="Missing closing quote [\"]">"This literal contains odd number of backslashes\\\\\\\"</error>

"""Hello world!"""
<error descr="Missing closing triple quote">"""Hello world!</error>