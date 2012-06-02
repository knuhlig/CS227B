package util.propnet.architecture.components;

import util.propnet.architecture.Component;

/**
 * The Or class is designed to represent logical OR gates.
 */
@SuppressWarnings("serial")
public final class Or extends Component
{
	/**
	 * Returns true if and only if at least one of the inputs to the or is true.
	 * 
	 * @see util.propnet.architecture.Component#getValue()
	 */
	@Override
	public boolean getValue()
	{
		if (valueIsCached()) {
			return getCachedValue();
		}
		for ( Component component : getInputs() )
		{
			if ( component.getValue() )
			{
				setCachedValue(true);
				return getCachedValue();
			}
		}
		setCachedValue(false);
		return getCachedValue();
	}

	/**
	 * @see util.propnet.architecture.Component#toString()
	 */
	@Override
	public String toString()
	{
		return toDot("ellipse", "grey", "OR");
	}
}