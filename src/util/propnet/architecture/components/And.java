package util.propnet.architecture.components;

import util.propnet.architecture.Component;

/**
 * The And class is designed to represent logical AND gates.
 */
@SuppressWarnings("serial")
public final class And extends Component
{
	/**
	 * Returns true if and only if every input to the and is true.
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
			if ( !component.getValue() )
			{
				setCachedValue(false);
				return getCachedValue();
			}
		}
		setCachedValue(true);
		return getCachedValue();
	}

	/**
	 * @see util.propnet.architecture.Component#toString()
	 */
	@Override
	public String toString()
	{
		return toDot("invhouse", "grey", "AND");
	}

}
