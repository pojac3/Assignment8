import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import java.util.stream.Collectors;

/**
 * CS 2614 Fall 2019 HW Assignment 7 Sample Solution
 * written by Dr. Nic "Doc" Grounds
 * 
 * Fulfills homework requirements same as project 07 of nand2tetris.org
 */
public class Assignment8
{
    public static void main( final String[] args )
        throws IOException
    {
        final File[] vmFiles = getVMSourceFiles( args[0] );
        final File asmFile = getASMDestinationFile( args[0] );
        final FileWriter writer = new FileWriter( asmFile );

        final Context c = new Context();

        for( final File vmFile : vmFiles ) {
            c.currentFilename = vmFile.getName().replace( ".vm", "" );
            getVMSourceCode( vmFile ).forEach( line -> {
                try {
                    c.lineNumber = line.getKey();
                    writer.write( getASMForVMCommand( line.getValue(), c ) );
                    writer.write( "\n" );
                }
                catch( final IOException ioe ) {
                    throw new RuntimeException( ioe );
                }
            } );
        }
        writer.close();
    }

    /**
     * Returns all *.vm File(s) associated with the given command-line argument
     * which can be a path / name of *.vm file itself, or a path / name of a
     * directory containing one or more *.vm files.
     */
    public static File[] getVMSourceFiles( final String arg )
    {
        File f = new File( arg );
        if( f.isDirectory() ) {
            return f.listFiles(
                (File d, String n) -> { return n.endsWith( ".vm" ); } );
        }

        return new File[] { f };
    }

    /**
     * Return the appropriate *.asm file destination for translated VM commands
     * for the given command-line argument which can be path / name of *.vm
     * file or directory containing one more *.vm files.
     */
    public static File getASMDestinationFile( final String arg )
    {
        File f = new File( arg );
        if( f.isDirectory() ) {
            return new File( f.getAbsolutePath() + ".asm" );
        }

        return new File( f.getParentFile(), f.getName().replace( ".vm", ".asm" ) );
    }

    /**
     * For the given *.vm File, return a Stream of SimpleEntry objects, the key
     * of which is the line number in the file and the value of which is the
     * VM command on the line.
     * 
     * Automatically filters and doesn't return blank or comment-only lines.
     */
    public static Stream<SimpleEntry<Integer,String>> getVMSourceCode( final File vmFile )
        throws FileNotFoundException
    {
        // AtomicInteger used as line counter from within Stream map Function
        final AtomicInteger counter = new AtomicInteger( 1 );
        return new BufferedReader( new FileReader( vmFile ) ).lines()
            // trim off comments
            .map( line -> line.contains( "//" ) ? line.substring( 0, line.indexOf( "//" ) ) : line )
            // convert line into (line number, line) SimpleEntry with trimmed line
            .map( line -> new SimpleEntry<Integer,String>( counter.getAndIncrement(), line.trim() ) )
            // filter out empty lines
            .filter( e -> !e.getValue().isEmpty() );
    }

    /**
     * For the given VM command (and Context) return a String of one or more
     * lines (separated by newlines) of ASM instructions which implement the VM
     * command.
     */
    public static String getASMForVMCommand( final String command, final Context c )
    {
        final String[] parts = command.split( " " );
        if( "push".equals( parts[0] ) ) {
            return getPush( parts[1], Integer.parseInt( parts[2] ), c );
        }
        else if( "pop".equals( parts[0] ) ) {
            return getPop( parts[1], Integer.parseInt( parts[2] ), c );
        }
        else if( ARITHMETIC_LOGIC_COMMANDS.containsKey( parts[0] ) ) {
            return ARITHMETIC_LOGIC_COMMANDS.get( parts[0] );
        }
        else if( "lt".equals( parts[0] ) || "gt".equals( parts[0] ) || "eq".equals( parts[0] ) ) {
            return makeTwoArgStackComparison( parts[0], c );
        }
        else if( "label".equals( parts[0]) ) {
            return makeLabel(parts[1]);
        }
        else if("goto".equals( parts[0] ) ) {
            return makeGoto(parts[1]);
        }
        else if("if-goto".equals( parts[0] ) ) {
            return makeIfGoto( parts[1] );
        }
        else if("function".equals( parts[0] ) ) {
            return makeFunction(parts[1],parts[2]);
        }
        else if("call".equals( parts[0] ) ) {
            return makeCall(parts[1],parts[2]);
        }
        throw new IllegalArgumentException( "Unknown command '" + command + "'" );
    }

    /**
     * makeLabel command
     * 
     * returns a String that is the labelName surrounded by parenthese
     * 
     */

     private static String makeLabel(String labelName) {
         return "(" + labelName + ")";
     }

     /**
      * makeGoto command
      * 
      * makes the assembly isntructions for Goto commands
      */

      private static String makeGoto(String where) {
          return String.join( "\n", 
          "@" + where, 
          "0;JMP");
      }
      /**
       * makeIfGoto command
       * 
       * Pops a value off the stack, loads it into the D register and jumps if not equal to zero
       */

        private static String makeIfGoto(String where) {
            return String.join( "\n",
            "@SP",
            "M=M-1",
            "A=M",
            "D=M",
            "@" + where,
            "D;JNE");
        }

      /**
       * makeFunction command
       * 
       * Makes function lines
       */

        private static String makeFunction(String name, String nLocals) {
            String returnString = "(" + name + ")";
          
            for (int i = 0; i < Integer.parseInt(nLocals); i++) {
                returnString += getPush(local, 0, c);
            }

            return returnString;
      }

      /**
       * makeCall command
       * 
       * Makes call lines
       */

       private static String makeCall(String function, String numberArgs) {
            String returnString = "";

            for (int i = 0; i < Integer.parseInt(numberArgs); i++) {
                returnString += String.join("\n", 
                "@SP",
                "M=M-1",
                "A=M",
                "D=M",
                "@ARG",
                "A=M",
                "M=D",
                "@ARG",
                "M=M+1");
            }
            returnString += String.join("\n",
            "@" + function,
            "0;JMP");

            returnString += makeLabel("functionCall." + function);

            return returnString;
       }

    /**
     * Helper class to hold context of a VM command being translated including:
     * 1) name of the *.vm file being processed
     * 2) line number of the VM command (used for helpful label generation)
     * 3) Set of known labels
     */
    private static class Context
    {
        String currentFilename;
        int lineNumber;
        Set<String> labels = new HashSet<>();

        private int i;
        String uniqueLabel( final String purpose )
        {
            String label = currentFilename + "_" + lineNumber + "_" + purpose + "_" + i++;
            while( labels.contains( label ) ) {
                label = currentFilename + "_" + lineNumber + "_" + purpose + "_" + i++;
            }

            return label;
        }
    }

    /**
     * Map of VM command memory segment names to information needed for handling
     * them in push and pop commands
     */
    private static final Map<String,Map.Entry<Integer,String>> MEMORY_SEGMENTS = new HashMap<>();
    static {
        // first four segments need to know their ASM symbol to use for
        // dereferencing their location
        MEMORY_SEGMENTS.put( "argument", new SimpleEntry<>( 0, "ARG" ) );
        MEMORY_SEGMENTS.put( "local", new SimpleEntry<>( 0, "LCL" ) );
        MEMORY_SEGMENTS.put( "this", new SimpleEntry<>( 0, "THIS" ) );
        MEMORY_SEGMENTS.put( "that", new SimpleEntry<>( 0, "THAT" ) );
        // next two segments need to know their hard-coded beginning memory
        // address which can never change
        MEMORY_SEGMENTS.put( "pointer", new SimpleEntry<>( 3, null ) );
        MEMORY_SEGMENTS.put( "temp", new SimpleEntry<>( 5, null ) );
        //constant and static are handled specially, beacuse they have no
        //mapping to a hard memory address
    }

    /**
     * Return the ASM instructions implementing a VM push command from the given
     * segment and index (within the given Context)
     */
    private static String getPush(
        final String segment,
        final int index,
        final Context c )
    {
        List<String> insts = new ArrayList<String>();
        if( MEMORY_SEGMENTS.containsKey( segment )
                && MEMORY_SEGMENTS.get( segment ).getKey() == 0 ) {
            insts.add( "@" + MEMORY_SEGMENTS.get( segment ).getValue() );
            insts.add( "A=M" );
            for( int i = index; i > 0; i-- ) {
                insts.add( "A=A+1" );
            }
            insts.add( "D=M" );
        }
        else if( MEMORY_SEGMENTS.containsKey( segment )
                && MEMORY_SEGMENTS.get( segment ).getValue() == null ) {
            insts.add( "@" + (MEMORY_SEGMENTS.get( segment ).getKey() + index) );
            insts.add( "D=M" );
        }
        else if( "constant".equals( segment ) ) {
            insts.add( "@" + index );
            insts.add( "D=A" );
        }
        else { //"static".equals( segment )
            insts.add( "@" + c.currentFilename + "_static_" + index );
            insts.add( "D=M" );
        }
        //increment SP and push D onto Stack
        insts.addAll( Arrays.asList( "@SP", "M=M+1", "A=M-1", "M=D" ) );

        return String.join( "\n", insts.toArray( new String[ insts.size() ] ) );
    }

    /**
     * Return the ASM instructions implementing a VM pop command to the given
     * segment and index (within the given Context)
     */
    private static String getPop(
        final String segment,
        final int index,
        final Context c )
    {
        //start with decrementing SP and read Stack top into D
        List<String> insts = new ArrayList<String>( Arrays.asList(
            "@SP", "AM=M-1", "D=M" ) );
        
        if( MEMORY_SEGMENTS.containsKey( segment )
                && MEMORY_SEGMENTS.get( segment ).getKey() == 0 ) {
            insts.add( "@" + MEMORY_SEGMENTS.get( segment ).getValue() );
            insts.add( "A=M" );
            for( int i = index; i > 0; i-- ) {
                insts.add( "A=A+1" );
            }
        }
        else if( MEMORY_SEGMENTS.containsKey( segment )
                && MEMORY_SEGMENTS.get( segment ).getValue() == null ) {
            insts.add( "@" + (MEMORY_SEGMENTS.get( segment ).getKey() + index) );
        }
        else { //"static".equals( segment )
            insts.add( "@" + c.currentFilename + "_static_" + index );
        }
        insts.add( "M=D" );

        return String.join( "\n", insts.toArray( new String[ insts.size() ] ) );
    }

    /**
     * Arithmetic and logic commands (less comparator commands which require
     * Context-sensitive translation to allow unique label synthesis)
     */
    private static final Map<String,String> ARITHMETIC_LOGIC_COMMANDS = new HashMap<>();
    static {
        ARITHMETIC_LOGIC_COMMANDS.put( "add", makeTwoArgStackCommand( "+" ) );
        ARITHMETIC_LOGIC_COMMANDS.put( "sub", makeTwoArgStackCommand( "-" ) );
        ARITHMETIC_LOGIC_COMMANDS.put( "and", makeTwoArgStackCommand( "&" ) );
        ARITHMETIC_LOGIC_COMMANDS.put( "or", makeTwoArgStackCommand( "|" ) );

        ARITHMETIC_LOGIC_COMMANDS.put( "neg", makeOneArgStackCommand( "-" ) );
        ARITHMETIC_LOGIC_COMMANDS.put( "not", makeOneArgStackCommand( "!" ) );
    }

    /**
     * Return ASM instructions for a simple two-arg stack operation, given that
     * replaces two Stack entries with a single entry, the value of which is
     * computed given some ALU operation from the two values.
     */
    private static String makeTwoArgStackCommand( final String op )
    {
        return String.join( "\n", "@SP", "AM=M-1", "D=M", "A=A-1", "M=M" + op + "D" );
    }

    /**
     * Return ASM instructions for a simple one-arg stack operation that
     * replaces the stack top value with one computed with a one-arg ALU
     * operation applied to that value.
     */
    private static String makeOneArgStackCommand( final String op )
    {
        return String.join( "\n", "@SP", "A=M-1", "M=" + op + "M" );
    }

    /**
     * Handle lt, gt, and eq VM commands by generate ASM instructions for that
     * op using appropriately unique synthesized label names using the given
     * Context.
     */
    private static String makeTwoArgStackComparison(
        final String op,
        final Context c )
    {
        final String l1 = c.uniqueLabel( op ), l2 = c.uniqueLabel( "not" + op );
        return String.join( "\n",
            "@SP", "AM=M-1", "D=M", "A=A-1", "D=M-D",
            "@" + l1, "D;J" + op.toUpperCase(),
            "@SP", "A=M-1", "M=0", "@" + l2, "0;JMP",
            "(" + l1 + ")", "@SP", "A=M-1", "M=-1",
            "(" + l2 + ")" );
    }
}