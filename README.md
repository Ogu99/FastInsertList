# Fast Insert List
A fast list implementation for the java.util.Collections Framework.
<p>
This is a little fun project created with the goal to implement a faster list than the LinkedList and the ArrayList. The
  best case <b>time complexity</b> for the methods <i>add, remove, get</i> are:
  <ul>
    <li>add - O(1)
    <li>get - O(1)
    <li>remove - O(1)
  </ul>
  and the worst cases are (n = amount of nodes, a = length of array inside the node):
  <ul>
    <li>add - O(n * a)
    <li>get - O(n)
    <li>remove - O(n * a)
  </ul>

Feel free to use it in your own programms.
