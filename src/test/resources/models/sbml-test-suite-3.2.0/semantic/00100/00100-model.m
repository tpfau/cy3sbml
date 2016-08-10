(* 

category:      Test
synopsis:      Basic two reactions with three species in one 0D compartment 
               and one functionDefinition.
componentTags: Compartment, Species, Reaction, Parameter, FunctionDefinition 
testTags:      Amount, 0D-Compartment
testType:      TimeCourse
levels:        2.1, 2.2, 2.3, 2.4, 2.5, 3.1
generatedBy:   Numeric

The model contains one compartment called "compartment".  There are three
species named S1, S2 and S3 and two parameters named k1 and k2.
Compartment "compartment" is 0-dimensional.  The model contains two
reactions defined as:

[{width:30em,margin: 1em auto}|  *Reaction*  |  *Rate*  |
| S1 + S2 -> S3 | $multiply(k1, S1, S2)$  |
| S3 -> S1 + S2 | $k2 * S3$  |]


The model contains one functionDefinition defined as:

[{width:30em,margin: 1em auto}|  *Id*  |  *Arguments*  |  *Formula*  |
 | multiply | x, y, z | $x * y * z$ |]


The initial conditions are as follows:

[{width:30em,margin: 1em auto}|      |*Value*          |*Units*  |
|Initial amount of S1               |$1.0 \x 10^-4$ |mole                      |
|Initial amount of S2               |$2.0 \x 10^-4$ |mole                      |
|Initial amount of S3               |$1.0 \x 10^-4$ |mole                      |
|Value of parameter k1              |$         0.75$ |mole^-1^ second^-1^ |
|Value of parameter k2              |$         0.25$ |second^-1^ |]

In this example, the compartment has its spatialDimensions attribute set to
zero; i.e., it represents a point and therefore cannot have size or units.
The species values must be treated as amounts and not concentrations.

*)

newcase[ "00100" ];

addFunction[ multiply, arguments -> {x, y, z}, math -> x * y * z];
addCompartment[ compartment, spatialDimensions-> 0 ];
addSpecies[ S1, initialAmount -> 1.0 10^-4];
addSpecies[ S2, initialAmount -> 2.0 10^-4];
addSpecies[ S3, initialAmount -> 1.0 10^-4];
addParameter[ k1, value -> 0.75 ];
addParameter[ k2, value -> 0.25 ];
addReaction[ S1 + S2 -> S3, reversible -> False,
	     kineticLaw -> multiply[k1, S1, S2]];
addReaction[ S3 -> S1 + S2, reversible -> False,
	     kineticLaw -> k2 * S3 ];

makemodel[]
