===========
Style Guide
===========

This document defines a style guide for Smithy models. Adhering to common
style guide makes models easier to read.

.. contents:: Table of contents
    :depth: 2
    :local:
    :backlinks: none


Model files
===========

Smithy models SHOULD be authored using the :ref:`Smithy IDL <smithy-language-specification>`.
Smithy models SHOULD resemble the following example:

.. code-block:: smithy

    $version: "0.4.0"

    metadata validators = []
    metadata suppressions = []

    namespace smithy.example.namespace

    /// This is the documentation
    @sensitive
    string MyShape

    /// This is the documentation.
    integer AnotherShape

    /// Documentation about the structure.
    ///
    /// More descriptive documentation if needed...
    structure MyStructure {
        /// Documentation about the member.
        @required
        foo: String,
    }

    // Example of creating custom traits.
    @trait(selector: "string")
    structure myTrait {}


File encoding
-------------

Smithy models are encoded in UTF-8.


New lines
---------

New lines are represented using ``Line Feed``, ``U+000A``.

All files SHOULD end with a new line.


One namespace per file
----------------------

Source model files SHOULD include only a single namespace. Multiple namespaces
MAY appear when representing a model as a single build artifact.


Model file structure
====================

A model file consists of, in order:

1. License or copyright information, if present
2. Smithy version number
3. :ref:`Metadata <metadata>`, if present
4. Namespaces

Exactly one blank line separates each section that is present.


Formatting
==========


Indentation
-----------

Models are indented using four spaces.


Whitespace
----------

1. A single space appears after a comma (",") and after a colon (":").
2. Spaces do not occur before a comma (",") or colon (":").
3. Lines do not end with trailing spaces.
4. Members of an object are not horizontally aligned.


Trailing commas
---------------

Include trailing commas to limit diff noise.


Naming
======


Shape names
-----------

Shape names use a strict form of UpperCamelCase (e.g., "XmlRequest", "FooId").


Member names
------------

Member names use a strict form of lowerCamelCase (e.g., "xmlRequest", "fooId").


Trait names
-----------

By convention, traits use lowerCamelCase (e.g., "xmlRequest", "fooId").

.. code-block:: smithy

    namespace smithy.example.namespace

    /// This is the documentation about the trait.
    ///
    /// This is more documentation.
    @trait(selector: "string")
    structure myTrait {}


Abbreviations
-------------

Abbreviations are represented as normal words. For example, use
"XmlHttpRequest" instead of "XMLHTTPRequest". Even two-letter abbreviations
follow strict camelCasing: "fooId" is used instead of "fooID".


Namespace names
---------------

Namespace names should consist of lowercase letters, numbers, and dots.
Camel case words can be used to better control namespaces. For example,
aws.dynamoDB can be used instead of "aws.dynamodb" in order to better
influence how code is generated in languages that utilize namespaces
with uppercase characters.
