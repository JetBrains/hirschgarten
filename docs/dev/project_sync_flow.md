# Project sync flow

Sync is an action that syncs build tool project structure and IntelliJ project structure. The goal is to represent build tool's structure as closely as possible in IntelliJ (in the perfect universe it would be 1-1 mapping, but we don't live in such one).

Seems to be an easy task, right? Well, it's not! In our plugin it is split into multiple steps, each on different layers and places. Here we will go though all of them in the "chronological" order - the order it's actually executed.

this file will be updated, stay tuned @abrams