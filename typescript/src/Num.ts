export class Num {
  value: number

  constructor(n: number) {
    this.value = n
  }

  val(): number {
    return this.value
  }

  add(n2: Num): Num {
    this.value += n2.val()
    return this
  }

  toString(): string {
    return this.val().toString()
  }

  static addAll(numArr: Array<Num>): Num {
    return new Num(numArr.map((n) => n.val()).reduce((a, b) => a + b, 0))
  }
}

/***
 * Go to TypeScript's official website https://www.typescriptlang.org/docs/handbook/typescript-from-scratch.html to read more.
 *
 * The following text is copied from
 * https://github.com/microsoft/TypeScript-New-Handbook/blob/master/intros/TypeScript%20for%20the%20New%20Programmer.md
 * and is under the MIT License
 */

/*

  * TypeScript for the New Programmer
    * What is JavaScript?
      * Runs everywhere
      * Untyped, error-prone language
      * Super common

# TypeScript for the New Programmer

Congratulations choosing TypeScript as one of your first languages - you're already making good decisions.

You've probably already heard that TypeScript is a "flavor" or "variant" of JavaScript.
The relationship between TypeScript and JavaScript is rather unique among modern programming languages, so learning more about this relationship will help you understand how TypeScript adds to JavaScript.

## What is JavaScript? A Brief History

JavaScript started as a simple scripting language available in browers.
When the language was first invented, writing more than a few dozen lines of JavaScript (JS) in a webpage would have been somewhat unusual.
Over time, though, JS became more common and web developers used more and more JS in their webpages to create interactive experiences.
Early web browsers executed JS very slowly and this sometimes led to laggy, unresponsive pages.

Web browser developers saw the increased use of JS and started to optimize their browsers to run JS more quickly.
Web developers responded by using JS even more to increase the interactivity of their pages.
On modern websites, you might be running tens or even hundreds of thousands of lines of JS code.
The web, which started as a simple network of static pages, has evolved into a platform for rich *applications* of all kinds.

Lately, JS has also become a popular choice for non-browser scenarios, such as running servers using node.js.
The "run anywhere" nature of JS makes it an attractive choice for cross-platform development.
Many developers these days use *only* JavaScript to program their entire stack!

The net result is that a very simple language designed to provide basic webpage interactivity is now being used to write applications with millions of lines of code.

JavaScript's humble beginnings are still visible in its many *quirks* - oddities or surprises that are not typical for other programming languages.
For example, JavaScript's equality operator `==` *coerces* its arguments, leading to unexpected behavior:
```js
if ("" == 0) {
    // It is! But why??
}
```

JavaScript also allows accessing properties which aren't present:
```js
const obj = { width: 10, height: 15 };
// Why is this NaN? Spelling is hard!
const area = obj.width * obj.heigth;
```

Other programming languages would usually cause these programs to either fail to start at all, or terminate the program mid-execution, if these sorts of errors occur.
When writing small programs, these quirks are annoying but manageable; when writing applications with hundreds or thousands of lines of code, these constant surprises make development slow and frustrating.

## TypeScript: A Static Type Checker

We said earlier that other programming languages wouldn't allow those buggy programs to run at all.
Detecting errors in code without running it is referred to as *static* checking.
Determining what's an error and what's not based on the kinds of values being operated on is known as *type* checking.

Because TypeScript checks a program for errors before it's run, and does so based on the *kinds of values*, it's a *static type checker*.
For example, the snippet from earlier has an error because of the *type* of `obj`.
Here's the error TypeScript found:

```ts
const obj = { width: 10, height: 15 };
const area = obj.width * obj.heigth;
```

### A Typed Superset of JavaScript

How does TypeScript relate to JavaScript, though?

#### Syntax

TypeScript is a programming language where all JavaScript syntax is legal.
Syntax refers to the way we arrange characters to form a program.
For example, this code has a *syntax* error because it's missing a `)`:

```js
let a = (4
```

TypeScript doesn't consider any JavaScript code to be an error because of its syntax.
This means you can take any working JavaScript code and put it in a TypeScript file without worrying about exactly how it's written.

#### Types

However, TypeScript is a *typed* superset, meaning that it adds rules about how different kinds of values can be used.
The earlier error about `obj.heigth` was not an error about the *syntax* of the program, but instead an error about using a kind of value (a *type*) in an incorrect way.

As another example, this is JavaScript code you can run in a browser that *will* print a value:
```js
console.log(4 / []);
```

This syntactically-legal program prints `NaN`.
TypeScript, though, considers division of number by an array to be a nonsensical operation, and will issue an error:

```ts
console.log(4 / []);
```

It's entirely possible you really did intend to divide a number by an array, perhaps just to see what would happen.
The vast majority of the time, though, this code is a mistake on the programmer's part.
TypeScript's type system rules are designed to allow correct programs through while still catching as many common errors as possible.
Later, we'll learn about settings you can use to configure how strictly TypeScript checks your code.

If you move some code from a JavaScript file to a TypeScript file, you might see *type errors* depending on how the code is written.
These may be legitimate problems with the code, or TypeScript being overly conservative.
Throughout this guide we'll demonstrate how to add various TypeScript syntax to make these errors go away.

#### Runtime Behavior

TypeScript is also a programming language that preserves the *runtime behavior* of JavaScript.
For example, dividing by zero in JavaScript produces `Infinity` instead of throwing a runtime exception.
TypeScript **never** changes the runtime behavior of JavaScript code.

This means code you move from JavaScript to TypeScript is **guaranteed** to do the same thing, even if TypeScript thinks that code has type errors.

Keeping the same runtime behavior as JavaScript is a foundational promise of TypeScript because it means you can easily transition between the two languages without worrying about subtle differences that might make your program stop working.

#### Erased Types

TypeScript's type system is *erased*, meaning that once your code is compiled, there is no persisted type information in the resulting JavaScript code.

TypeScript also never changes the behavior of your program based on the types it inferred.
While you might see more or fewer type errors, the type system itself has no bearing on how your program works once it's running.

Finally, TypeScript doesn't provide any additional runtime libraries.
Your programs will use the same standard library (or external libraries) as JavaScript programs, so there's no additional TypeScript-specific framework to learn.

## Learning JavaScript and TypeScript

We frequently see the question "Should I learn JavaScript, or TypeScript instead?".

The answer is that you can't do one without doing the other!
Because TypeScript shares syntax and runtime behavior with JavaScript, anything you learn about JavaScript is helping you learn TypeScript at the same time.

There are many, many resources available for programmers learning JavaScript, and you shouldn't ignore these resources if you're writing TypeScript.
For example, there about 20 times more StackOverflow questions tagged `javascript` than `typescript`, but *all* of the `javascript` questions also apply to TypeScript.

If you find yourself searching for something like "how to sort a list in TypeScript", remember, **TypeScript is JavaScript's runtime with a build-time type system**.
The way you sort a list in TypeScript is the same way you sort a list in JavaScript.
If you find a resource that does use TypeScript, that's great too, but don't limit yourself to thinking you need TypeScript-specific answers for everyday questions about how to accomplish runtime tasks.

*/
