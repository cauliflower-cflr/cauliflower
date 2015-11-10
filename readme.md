Cauliflower
===========

__C__auli__FL__owe__R__ is a Context-Free Language Reachability (CFL-R) engine.

It is currently under active development.
Check this space for updates/release notes.

Architecture
------------

Modular as possible:

 * Problem <templated>: definition of the problem templated by evaluation specification
 * Relation: a group of ADTs
 * ADT: generic template for the abstract datatype:
     * dense representations
     * neighbourhood maps
     * quadtrees
 * Relation Buffer: buffer for reading in relational information

Development/Contribution
------------------------

Contributions are always welcome :)

Milestones
----------

__0.1__:
Primary release.
Support for problem definition and evaluation.
Limited support for input/extensibility.

__0.2__:
Intersection semantics.
