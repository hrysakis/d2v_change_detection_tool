/**
 *
 * @author Ioannis Chrysakis (hrysakis@ics.forth.gr)
 */

package utils;

import java.util.Comparator;
import org.diachron.detection.complex_change.SCDefinition;

/**
 * SortBasedOnName is a class that is used to compare lists of SCDefinition based on their name
 */
public class SortBasedOnName implements Comparator
{
 
/**
 * Compares two objects based on their names
 */    
public int compare(Object o1, Object o2) 
{

    SCDefinition dd1 = (SCDefinition)o1;// where FBFriends_Obj is your object class
    SCDefinition dd2 = (SCDefinition)o2;
    return dd1.getsChangeUri().compareToIgnoreCase(dd2.getsChangeUri());//where uname is field name
}

}
