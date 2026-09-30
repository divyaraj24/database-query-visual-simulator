grammar SqlSubset;

// Entry points -------------------------------------------------------------

script
    : statement+ EOF
    ;

statement
    : createTableStatement SEMI?
    | selectStatement SEMI?
    ;

// CREATE TABLE ---------------------------------------------------------

createTableStatement
    : CREATE TABLE tableName LPAREN columnDef (COMMA columnDef)* RPAREN
    ;

columnDef
    : columnName dataType columnConstraint*
    ;

dataType
    : IDENTIFIER (LPAREN INT_LITERAL (COMMA INT_LITERAL)? RPAREN)?
    ;

columnConstraint
    : PRIMARY KEY
    | NOT NULL_KW
    | UNIQUE
    | REFERENCES tableName (LPAREN columnName RPAREN)?
    ;

// SELECT -----------------------------------------------------------------

selectStatement
    : SELECT selectList
      FROM fromItem
      joinClause*
      whereClause?
      orderByClause?
    ;

selectList
    : STAR
    | selectItem (COMMA selectItem)*
    ;

selectItem
    : qualifiedColumn (AS? IDENTIFIER)?
    ;

fromItem
    : tableName (AS? IDENTIFIER)?
    ;

joinClause
    : joinType JOIN fromItem ON joinCondition
    ;

joinType
    : INNER?
    | LEFT OUTER?
    | RIGHT OUTER?
    ;

joinCondition
    : qualifiedColumn EQ qualifiedColumn
    ;

whereClause
    : WHERE predicate (AND predicate)*
    ;

predicate
    : qualifiedColumn comparisonOp literal
    ;

comparisonOp
    : EQ | NEQ | LT | LTE | GT | GTE
    ;

orderByClause
    : ORDER BY orderItem (COMMA orderItem)*
    ;

orderItem
    : qualifiedColumn (ASC | DESC)?
    ;

// Shared -------------------------------------------------------------------

qualifiedColumn
    : (IDENTIFIER DOT)? columnName
    ;

tableName : IDENTIFIER ;
columnName : IDENTIFIER ;

literal
    : STRING_LITERAL
    | INT_LITERAL
    | DECIMAL_LITERAL
    ;

// Lexer ----------------------------------------------------------------

CREATE: [Cc][Rr][Ee][Aa][Tt][Ee];
TABLE: [Tt][Aa][Bb][Ll][Ee];
SELECT: [Ss][Ee][Ll][Ee][Cc][Tt];
FROM: [Ff][Rr][Oo][Mm];
WHERE: [Ww][Hh][Ee][Rr][Ee];
JOIN: [Jj][Oo][Ii][Nn];
INNER: [Ii][Nn][Nn][Ee][Rr];
LEFT: [Ll][Ee][Ff][Tt];
RIGHT: [Rr][Ii][Gg][Hh][Tt];
OUTER: [Oo][Uu][Tt][Ee][Rr];
ON: [Oo][Nn];
ORDER: [Oo][Rr][Dd][Ee][Rr];
BY: [Bb][Yy];
ASC: [Aa][Ss][Cc];
DESC: [Dd][Ee][Ss][Cc];
AND: [Aa][Nn][Dd];
AS: [Aa][Ss];
PRIMARY: [Pp][Rr][Ii][Mm][Aa][Rr][Yy];
KEY: [Kk][Ee][Yy];
NOT: [Nn][Oo][Tt];
NULL_KW: [Nn][Uu][Ll][Ll];
UNIQUE: [Uu][Nn][Ii][Qq][Uu][Ee];
REFERENCES: [Rr][Ee][Ff][Ee][Rr][Ee][Nn][Cc][Ee][Ss];

STAR: '*';
COMMA: ',';
DOT: '.';
SEMI: ';';
LPAREN: '(';
RPAREN: ')';
EQ: '=';
NEQ: '<>' | '!=';
LT: '<';
LTE: '<=';
GT: '>';
GTE: '>=';

IDENTIFIER: [a-zA-Z_][a-zA-Z_0-9]*;
INT_LITERAL: [0-9]+;
DECIMAL_LITERAL: [0-9]+ '.' [0-9]+;
STRING_LITERAL: '\'' (~['\r\n])* '\'';

WS: [ \t\r\n]+ -> skip;
LINE_COMMENT: '--' ~[\r\n]* -> skip;
