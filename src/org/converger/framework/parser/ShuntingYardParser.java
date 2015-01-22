package org.converger.framework.parser;


import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.List;
import java.util.Stack;

import org.converger.framework.Environment;
import org.converger.framework.core.Operator;

/**
 * This class implements a parser for expressions written in infix notation.
 * Internally, it uses Dijkstra's shunting-yard algorithm.
 * Produces a list of strongly typed tokens, ordered in reverse polish notation (RPN/postfix).
 * @author Dario Pavllo
 */
public class ShuntingYardParser implements Parser {
	
	private final Stack<Token> output;
	private final Stack<Token> stack;
	
	/**
	 * Initializes this parser.
	 */
	public ShuntingYardParser() {
		this.output = new Stack<>();
		this.stack = new Stack<>();
	}
	
	/**
	 * Returns the list of parsed tokens, in postfix notation (RPN).
	 * @return a list containing the parsed tokens
	 */
	@Override
	public List<Token> getOutputList() {
		return new ArrayList<>(this.output);
	}
	
	@Override
	public void parse(final Iterable<String> input) {
		for (final String token : input) {
			if (ShuntingYardParser.isNumber(token)) {
				//Number: push it on the output stack
				this.output.push(new Token(token, Token.Type.NUMBER));
			} else if (token.matches("[a-zA-Z]+")) {
				//Function or variable
				if (Environment.getSingleton().hasFunction(token)) {
					this.stack.push(new Token(token, Token.Type.FUNCTION));
				} else {
					//If there is no function which has the given name,
					//it is treated as a variable
					this.output.push(new Token(token, Token.Type.VARIABLE));
				}
			} else if (token.length() == 1) { //NOPMD
				//It can be either a parenthesis, or an operator
				if (token.equals(Token.LEFT_PARENTHESIS.getContent())) {
					//If it's a left parenthesis, push it on the operator stack
					this.stack.push(Token.LEFT_PARENTHESIS);
				} else if (token.equals(Token.RIGHT_PARENTHESIS.getContent())) {
					try {
						//Pops tokens from the operator stack until a left
						//parenthesis is found.
						while (!stack.peek().equals(Token.LEFT_PARENTHESIS)) {
							//Transfers the token to the output stack
							output.push(stack.pop());
						}
						stack.pop(); //Pops the left parenthesis
						
					} catch (final EmptyStackException e) {
						//If no left parentheses are found, they are mismatched
						throw new IllegalArgumentException("Mismatched parentheses", e);
					}
					if (!stack.isEmpty() && stack.peek().getType() == Token.Type.FUNCTION) {
						output.push(stack.pop());
					}
				} else {
					//If it's not a parenthesis, it's an operator
					this.pushOperator(this.getOperator(token));
				}
			}
		}
		this.complete();
	}
	
	private void pushOperator(final Operator o) {
		boolean hasOperator = true;
		//Repeat until there is an operator on top of the stack
		while (hasOperator && !this.stack.isEmpty()) {
			final Token topToken = this.stack.peek();
			if (topToken.getType() == Token.Type.OPERATOR) {
				//Compares the precedence of the two last operators
				final Operator topOperator = this.getOperator(topToken.getContent());
				if (o.getPrecedence() <= topOperator.getPrecedence()) {
					this.stack.pop();
					this.output.push(topToken);
				} else {
					hasOperator = false;
				}
			} else {
				hasOperator = false;
			}
		}
		//Pushes the operator onto the top of the operator stack
		this.stack.push(new Token(o.getSymbol(), Token.Type.OPERATOR));
	}
	
	private void complete() {
		//Pops the remaining tokens from the operator stack
		while (!this.stack.isEmpty()) {
			final Token top = this.stack.pop();
			//If the token is a parenthesis, there is something wrong
			if (top.getType() == Token.Type.LEFT_PARENTHESIS
					|| top.getType() == Token.Type.RIGHT_PARENTHESIS) {
				throw new IllegalArgumentException("Mismatched parentheses");
			}
			output.push(top);
		}
	}
	
	private static boolean isNumber(final String str) {
		try {
			Double.parseDouble(str);
			return true;
		} catch (final NumberFormatException e) {
			return false;
		}
	}
	
	private Operator getOperator(final String token) {
		if (!Environment.getSingleton().hasOperator(token)) {
			throw new IllegalArgumentException("Unknown operator: " + token);
		}
		return Environment.getSingleton().getOperator(token);
	}
}