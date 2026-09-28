package Patterns.CompositePattern;

interface Expression {
  int evaluate();
}

class Number implements Expression {
  private int value;

  public Number(int value) {
    this.value = value;
  }

  public int evaluate() {
    return value;
  }
}


class OperationExpression implements Expression {
  private Expression expression1;
  private Expression expression2;
  private String operation;

  public OperationExpression(Expression expression1, Expression expression2, String operation) {
    this.expression1 = expression1;
    this.expression2 = expression2;
    this.operation = operation;
  }

  public int evaluate() {
    if(operation.equals("add")){
      return expression1.evaluate() + expression2.evaluate();
    }else if(operation.equals("sub")){
      return expression1.evaluate() - expression2.evaluate();
    }else if(operation.equals("mul")){
      return expression1.evaluate() * expression2.evaluate();
    }else if(operation.equals("div")){
      return expression1.evaluate() / expression2.evaluate();
    }
    return 0;
  }
}

public class Calculator {
  public static void main(String[] args) {
    Expression expression1 = new Number(1);
    Expression expression2 = new Number(2);
    Expression expression3 = new Number(3);
    Expression expression4 = new Number(4);
    Expression expression5 = new OperationExpression(expression1, expression2, "add");
    Expression expression6 = new OperationExpression(expression3, expression4, "add");
    Expression expression7 = new OperationExpression(expression5, expression6, "add");
    System.out.println(expression7.evaluate());
  }
}
