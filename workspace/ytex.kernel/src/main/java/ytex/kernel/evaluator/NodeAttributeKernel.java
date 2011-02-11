package ytex.kernel.evaluator;

import ytex.kernel.tree.Node;

public class NodeAttributeKernel implements Kernel {

	private Kernel delegateKernel;
	private String attributeName;

	public String getAttributeName() {
		return attributeName;
	}

	public void setAttributeName(String attributeName) {
		this.attributeName = attributeName;
	}

	public Kernel getDelegateKernel() {
		return delegateKernel;
	}

	public void setDelegateKernel(Kernel delegateKernel) {
		this.delegateKernel = delegateKernel;
	}

	@Override
	public double evaluate(Object o1, Object o2) {
		Node n1 = (Node) o1;
		Node n2 = (Node) o2;
		if (n1 != null && n2 != null && n1.getType().equals(n2.getType())
				&& n1.getValue().get(attributeName) != null
				&& n2.getValue().get(attributeName) != null) {
			return delegateKernel.evaluate(n1.getValue().get(attributeName), n2
					.getValue().get(attributeName));
		} else {
			return 0;
		}
	}
}
